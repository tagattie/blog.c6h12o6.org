---
title: "How to port an Electron-based application to FreeBSD"
date: 2020-02-27T14:25:47+09:00
categories: ["FreeBSD"]
tags: ["freebsd", "ports", "electron"]
draft: true
---

[Electron](https://www.electronjs.org) is a popular framework for developing desktop applications using web technologies like HTML, CSS, and JavaScript. There are many applications (including both open source and closed source) built on top of Electron and some of them can be found at the following websites:
- [Electron Apps](https://www.electronjs.org/apps)
- [Awesome Electron](https://github.com/sindresorhus/awesome-electron)

After being listed on [Wanted Ports](https://wiki.freebsd.org/WantedPorts) for some time, Electron landed on the FreeBSD's Ports tree in May 2019 and two major versions (4 and 6) are currently supported. Two Electron-based applications, both of which happen to be code editors, [Visual Studio Code](https://www.freshports.org/editors/vscode/) and [Atom](https://www.freshports.org/editors/atom/), have been added to the Ports since then.

If you look into those ports, you will find a lengthy Makefile and a lot of patch files. So, you may think porting an Electron-based application is a laborious work. However, it is not the case usually. Those two editors are outliers and porting an Electron-based application should be easier than you think.

With some [helper makefiles](https://github.com/tagattie/FreeBSD-Electron/tree/master/Mk/Uses) (still work-in-progress), you can make a port by writing less than a hundred lines of Makefile and some companion files.

In this post, I will describe how you can make use of those `.mk` files for porting an Electron-based application. I pick a simple (but still useful) application, which is [YouTube Music Desktop](https://github.com/ytmdesktop/ytmdesktop), for illustration purposes.

## Prerequisites
Before starting porting, we need to do some preparatory work. First, we need information about the application such as:
- The version of Electron the application depends,
- The version of Node needed for building the application, and
- The Node package manager needed for building the application.

A README document and/or a file called `package.json` in the application's source archive usually provide such information. Let's download the archive and look into it.
``` shell
fetch https://github.com/ytmdesktop/ytmdesktop/archive/v1.8.2.tar.gz
tar -xzf v1.8.2.tar.gz
less ytmdesktop-1.8.2/package.json
```

- Electron version --- Electron is usually specified as a development dependency of an application project. Look into `devDependencies` section of `package.json` and we find the line `"electron": "^7.1.11",`. The version of Electron needed is **`7`**.
- Node version --- We don't find specific descriptions about Node in the archive. So, we assume any supported Node versions can be used. In our case, let's use version **`12`**. (**NOTE**: This is just because I like to use the latest LTS (Long Term Support) version and any other supported versions should work.)
- Package manager --- An application project usually uses a file for locking down all the versions of its dependencies (node modules) for project reproducibility. The file `package-lock.json` is for the package manager "NPM" and `yarn.lock` for "Yarn". In our example, the archive has both files, which probably means either package manager can be used. We choose NPM as the package manager (at my discretion :-).

In summary, we will use:
- Electron: **7**
- Node: **12**
- Package manager: **NPM**

One more step of preparation is necessary. As mentioned above, the port will use features provided by the work-in-progress `.mk` files. So, let's obtain those files and copy them into the ports directory.
``` shell
git clone https://github.com/tagattie/FreeBSD-Electron.git
cp FreeBSD-Electron/Mk/Uses/*.mk ${PORTSDIR}/Mk/Uses
```

Now we're all set to start making a port.

## Porting
**NOTE**: The complete port is available at [my forked Ports repository](https://github.com/tagattie/freebsd-ports/tree/master/multimedia/ytmdesktop). If you quickly want to see what it looks, please head over there.

### Initialize the port
First, initialize the port, for example, by executing the following commands. (**NOTE**: `ports-mgmt/porttools` is used here.)
``` shell
cd ${PORTSDIR}
port create multimedia/ytmdesktop
```

### Put package.json and lock file
Copy the files `package.json` and `package-lock.json` in the application's source archive into the port's `files/packagejsons` directory.
``` shell
cd multimedia/ytmdesktop
mkdir -p files/packagejsons
cp /path/to/archive/ytmdesktop-1.8.2/package*.json files/packagejsons
```

Why do we need this?

The most troublesome part of porting an Electron-based application I guess is preparation of distribution files. An official port is required to be able to be built with Poudriere, which means all distribution files (including all node modules the application depends on) must be downloaded in advance and verified their integrity with the checksum file (`distinfo`).

A project using NPM specifies all module dependencies in `package.json` and `package-lock.json`. The helper `.mk` files make use of those files to "pre-fetch" all node modules needed by the application and tar them up into a single archive file subject to checksum verification.

So, you don't have to manually handle distribution files for the node modules. The Ports infrastructure takes care of naming, fetching and making checksum of those node modules (when you execute `make makesum`).

### Write Makefile
Now, let's prepare the Makefile. I will describe Electron-related parts only. For the complete Makefile, please check [my forked repo](https://github.com/tagattie/freebsd-ports/blob/master/multimedia/ytmdesktop/Makefile).

There are three important variables we need to specify, which are `USES`, `USE_NODE`, and `USE_ELECTRON`. First, let's look at `USES` and `USE_NODE`. By defining those two variables, the specified versions of Electron and Node, and the specified packager manager are automatically added to necessary dependencies.
``` makefile
USES=           electron:7 node:12,build
USE_NODE=       npm
```

The next variable `USE_ELECTRON` is about the heart of the features provided. So, I will explain these features in detail.
``` makefile
USE_ELECTRON=   prefetch extract prebuild build:builder
PREFETCH_TIMESTAMP=     1582793516
```

- `prefetch` --- When specified, if a distribution file does not exist in `${DISTDIR}`, the fetch phase downloads all node modules the application depends on using pre-stored `package.json` and `package-lock.json`. Downloaded node modules are archived into an automatically-named single tar file as one of `DISTFILES`.
	- `PREFETCH_TIMESTAMP` --- If `prefetch` feature is used, this variable must be defined. The variable is a timestamp given to every directory, file or link in the tar archive, which is necessary for reproducibility of the archive. You can use the command `date '+%s'` to obtain a value for this.
- `extract` --- When specified, the extract phase installs the pre-fetched node modules into the port's working source directory.
- `prebuild` --- When specified, the build phase rebuilds native node modules against the specified version of Node so that Node can execute the native modules for building the application. In addition, the feature enables rebuilding native node modules against the specified version of Electron before application packaging.

Roughly speaking, with those three features, the port build process executes operations equivalent to `npm install` divided into three phases.

The last feature is about application packaging which generates a distributable form of the application. According to [the website](https://www.electronjs.org/docs/tutorial/application-distribution), there are the following popular tools:
- [electron-forge](https://www.electronforge.io/)
- [electron-builder](https://www.electron.build/)
- [electron-packager](https://npm.im/electron-packager)

The feature currently supports electron-builder and electron-packager (`build:builder` and `build:packager` respectively).

A packaging tool is usually specified as a development dependency. Look into `devDependencies` of `package.json` and we find the line `"electron-builder": "^21.2.0",`. So, the application depends on electron-builder.

We are almost finished, but one last thing remains. Preparing the install phase is the most tedious work. You will have to write the installation target from scratch. Required processes are:
- create a wrapper script for the application,
- create a `.desktop` entry for the application,
- generate icons for the application (unless there are in the source archive already), and
- write `do-install` target in the Makefile.

A template of the wrapper script will look something like this:
``` shell
#! /bin/sh

export NODE_ENV=production
export ELECTRON_IS_DEV=0

electron%%ELECTRON_VER_MAJOR%% %%DATADIR%%/resources/app.asar $@
```

Write `do-install` target to manually install those created wrapper script, `.desktop` file, and icons. Don't forget to copy the application resources directory generated by the packaging tool to `${DATADIR}`.
``` makefile
do-install:
        # installation of wrapper script, .desktop entry and icons
        # to appropriate locations
        (snip)
        # install application data directory to ${DATADIR}
        ${MKDIR} ${STAGEDIR}${DATADIR}
        cd ${WRKSRC}/dist/linux-unpacked && \
                ${COPYTREE_SHARE} resources ${STAGEDIR}${DATADIR}
```

## Building
Finally, we are ready to build the port.

``` shell
make makesum # to generate distinfo
make build
```

We still need `pkg-descr` and `pkg-plist` files to make a package of the application. I would like to leave them for you since needed work is not Electron-specific.
