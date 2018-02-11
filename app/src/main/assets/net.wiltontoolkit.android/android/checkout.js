/*
 * Copyright 2017, alex at staticlibs.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define([], function() {

    return {
        main: function(url, passwd, branch, appdirPath) {
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
