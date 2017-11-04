
// git clone/checkout/update logic for app JS code for development
// credentials can be changed on-device in /sdcard/Android/data/<package>/files
(function() {
    var GIT_ENABLE = true
    var GIT_URL = "git+ssh://androiddev@192.168.43.165/home/androiddev/android-app";
    var GIT_PASSWORD = "androiddev";
    var GIT_BRANCH = "master";

    if (GIT_ENABLE) {
        // application directory
        var appdir = new Packages.java.io.File(GLOBAL_ACTIVITY.getExternalFilesDir(null), "app");

        // JGit setup for SSH
        var sf = new Packages.net.wiltontoolkit.support.jgit.PasswordSshSessionFactory(GIT_PASSWORD)
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

        // show revision
        //var revision = repo.resolve(Packages.org.eclipse.jgit.lib.Constants.HEAD);
        //GLOBAL_ACTIVITY.showMessage("git: " + revision.name());

        // close repo
        repo.close();
    }

    // init app
    var appinitJs = new Packages.java.io.File(GLOBAL_ACTIVITY.getExternalFilesDir(null), "app/init.js");
    GLOBAL_ACTIVITY.runJsFile(appinitJs);
}());
