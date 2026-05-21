TAG=$1
[ -z "$TAG" ] && echo "Usage: $0 TAG" && exit 1

git tag -d $TAG
git push origin :refs/tags/$TAG
git tag $TAG
git push origin $TAG
