#!/bin/bash
TAG=$1
[ -z "$TAG" ] && echo "Usage: $0 TAG" && exit 1
[[ ! "$VERSION" =~ ^v[0-9]+\.[0-9]+$ ]] && echo "Usage: $0 v#.#" && exit 1
git tag -d $TAG
git tag $TAG
git push github $TAG --force
