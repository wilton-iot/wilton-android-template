
// git clone/checkout/update logic for app JS code for development
// credentials can be changed on-device in /sdcard/Android/data/<package>/files
// checkout/update logic may be moved to app/index.js instead
(function() {
    GIT_ENABLE = false
    GIT_URL = "git+ssh://androiddev@192.168.1.7/home/androiddev/my-wilton-app";
    GIT_PASSWORD = "androiddev";
    GIT_BRANCH = "master";

    if (GIT_ENABLE) {
        // application directory
        var appdir = new Packages.java.io.File(GLOBAL_APP.getWorkDirectory(), "app");

        // JGit setup for SSH
        var sf = new Packages.net.wiltonwebtoolkit.support.jgit.PasswordSshSessionFactory(GIT_PASSWORD)
                .withStrictHostKeyChecking(false);
        Packages.org.eclipse.jgit.transport.SshSessionFactory.setInstance(sf);

        // git clone app
        if (!(appdir.exists() && appdir.isDirectory())) {
            Packages.org.eclipse.jgit.api.Git.cloneRepository()
                    .setURI(GIT_URL)
                    .setDirectory(appdir)
                    .call();
        }

        // find out repo
        var repo = new Packages.org.eclipse.jgit.storage.file.FileRepositoryBuilder()
                .setGitDir(new Packages.java.io.File(appdir, ".git"))
                .setMustExist(true)
                .build();
        var git = new Packages.org.eclipse.jgit.api.Git(repo);

        // git checkout
        git.checkout()
                .setName(GIT_BRANCH)
                .setUpstreamMode(Packages.org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                .setStartPoint("origin/" + GIT_BRANCH)
                .call();

        // git update
        git.pull().call();
    }
}());

// run app
load(GLOBAL_APP.getWorkDirectory() + "/app/index.js");
