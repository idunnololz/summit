# This is probably not the page/README you are looking for

## Are you interested in finding out more about Summit? 

https://summit.idunnololz.com

## Are you an existing user who wants to report a bug/suggest a feature? Or just get updates about the app?

https://lemmy.world/c/summit

## Are you a developer who is here to check things out?

Please note that I am currently not accepting PRs. This repo is still mostly meant for me so 
documentation might be rough around the edges.

If you have any technical questions you can ask on https://lemmy.world/c/summit.

# Summit

This is the repository that contains the source code of Summit for Lemmy.

**Below is where I keep notes to myself so if it seems like non-sense that's why.**

## Release process

1. Create a new release post on https://lemmy.world/c/summit. Feature post.
2. Update [Changelog.kt] with the release post id.
3. Push latest translations: https://translate.idunnololz.com/projects/summit/summit/#repository.
4. Merge latest translations to main. (https://github.com/idunnololz/summit)
5. Pull latest translations on local.
6. Check patreon and update patreon list.
7. Bump version code and string.
8. Create a release build.
9. Add to play store.
10. Commit & tag release.

### Update translation credit

1. Go to https://translate.idunnololz.com/projects/summit/summit/#reports
2. Choose report structure `json`.
3. Change starting date to 1/1/1999.
4. Change ending date to the current date.
5. Click generate.
6. Copy the result and paste the json into [app/src/main/res/raw/translate.json].

## Linter

To run the linter do
`./gradlew clean ktlintCheck`

To fix lint issues do
`./gradlew clean ktlintFormat`

## Updating API version

I diffed:
```ps
$dir1 = Get-ChildItem -Recurse -path ~\LemmyBackwardsCompatibleAPI-master\app\src\commonMain\kotlin\it\vercruysse\lemmyapi\v0\x19\x3
$dir2 = Get-ChildItem -Recurse -path ~\LemmyBackwardsCompatibleAPI-master\app\src\commonMain\kotlin\it\vercruysse\lemmyapi\v0\x19\x4
Compare-Object -ReferenceObject $dir1 -DifferenceObject $dir2
```

## Dependency updates

`./gradlew dependencyUpdates -Drevision=release`