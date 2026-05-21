VER=$(grep 'versionName = "' app/build.gradle | sed 's/"$//; s/.*"//')
APK=app/build/outputs/apk/release/com.wade.fsime-${VER}-release.apk
rm -f $APK
./gradlew assembleRelease 2>&1 | tee build.txt
sleep 1
if [ -f $APK ]; then
	adb install -r $APK
else
	echo "APK=$APK is not exist."
fi
