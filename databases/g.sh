#!/bin/bash
COUNT=0
while read ch; do
	COUNT=$((COUNT + 1))
	echo wget "https://pedia.cloud.edu.tw/Home/List/1?search=${ch}&source=withPrimary,withEOL,withMH,withConcise,withIdiom,withJunior,withRevised&order=relevance&size=100"
	\wget "https://pedia.cloud.edu.tw/Home/List/1?search=${ch}&source=withPrimary,withEOL,withMH,withConcise,withIdiom,withJunior,withRevised&order=relevance&size=100" -O ${COUNT}-${ch}.txt
	cat ${COUNT}-${ch}.txt | grep -rHn searchResultWord | sed 's/.*title=//g;s/&search.*//g' | grep -v click > ../words/${COUNT}-${ch}.txt
done
