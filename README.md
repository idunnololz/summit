# Summit

## Release process

1. Create a new release post on https://lemmy.world/c/summit. Feature post.
2. Update [Changelog.kt] with the release post id.
3. Push latest translations: https://translate.idunnololz.com/projects/summit/summit/#repository.
4. Merge latest translations to main.
5. Pull latest translations on local.
6. Bump version code and string.
7. Create a release build.
8. Add to play store.
9. Commit & tag release.

## Linter

To run the linter do
`./gradlew clean ktlintCheck`

To fix lint issues do
`./gradlew clean ktlintFormat`