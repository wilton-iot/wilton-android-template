
define([], function() {

    return {
        checkout: function(url, passwd, branch, appdirPath) {
            // JGit setup for SSH
            var sf = new Packages.net.wiltontoolkit.support.jgit.PasswordSshSessionFactory(passwd)
                    .withStrictHostKeyChecking(false);
            Packages.org.eclipse.jgit.transport.SshSessionFactory.setInstance(sf);

            // git clone app
            var appdir = new Packages.java.io.File(appdirPath);
            if (!(appdir.exists() && appdir.isDirectory())) {
                Packages.org.eclipse.jgit.api.Git.cloneRepository()
                        .setURI(url)
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
                    .setName(branch)
                    .setUpstreamMode(Packages.org.eclipse.jgit.api.CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setStartPoint("origin/" + branch)
                    .call();

            // git update
            git.pull().call();

            // show revision
            //var revision = repo.resolve(Packages.org.eclipse.jgit.lib.Constants.HEAD);
            //GLOBAL_ACTIVITY.showMessage("git: " + revision.name());

            // close repo
            repo.close();
        }
    };

});
