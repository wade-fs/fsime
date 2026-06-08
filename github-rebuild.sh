#!/bin/bash
TAG=$1
if [ -z "$TAG" ]; then
    TAG=$(grep "versionName =" app/build.gradle | sed 's/.*"\(.*\)".*/\1/')
    echo "Using version from app/build.gradle: $TAG"
fi

[ -z "$TAG" ] && echo "Usage: $0 [TAG]" && exit 1
! [[ "$TAG" =~ ^v[0-9]+(\.[0-9]+){1,2}$ ]] && echo "Error: Invalid tag format '$TAG'. Expected v#.# or v#.#.#" && exit 1
git tag -d $TAG >/dev/null 2>&1 || true
git push origin :refs/tags/$TAG
git tag $TAG
git push origin $TAG --force
