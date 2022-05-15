#!/bin/bash
COUNT=0
while read ch; do
	COUNT=$((COUNT + 1))
	curl "https://pedia.cloud.edu.tw/Home/List/1?search=${ch}&source=withPrimary,withEOL,withMH,withConcise,withIdiom,withJunior,withRevised&order=relevance&size=100" -o ${COUNT}-${ch}.txt
	cat ${COUNT}-${ch}.txt | grep -rHn 'a class="searchResultWord" index=' | sed 's/.*title=//g;s/&search.*//g' | grep ${ch} | grep -v '^.$' > ../words/${COUNT}-${ch}.txt
done
