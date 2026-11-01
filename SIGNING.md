# Lexicon: signing Android releases

The private signing key is NEVER committed to this public repository.

## One-time setup

Keep the following two private files provided with this release:
- lexicon-release-keystore.p12
- lexicon-signing-setup.txt

GitHub repository Settings → Secrets and variables → Actions → New repository secret.
Add the four named secrets and copy their values from lexicon-signing-setup.txt:

1. LEXICON_KEYSTORE_BASE64
2. LEXICON_KEYSTORE_PASSWORD
3. LEXICON_KEY_ALIAS
4. LEXICON_KEY_PASSWORD

After that, run Android CI on main using "Run workflow".
It produces both lexicon-debug-apk and lexicon-signed-release-apk artifacts.
The release APK is signed using the stable release key. Debug APKs are signed by
ephemeral CI debug keys and do NOT have a stable identity across builds.

## Important compatibility warning

Android refuses to update an installed app with an APK signed by a different
certificate, even if both APKs have the same package name. Do NOT delete an
installed Lexicon until its data has been backed up. The current version lacks
manual export/restore. The signing keystore and its password must be stored
safely for every future release. If they are lost, updates signed by the
same identity may no longer be possible.

Without secrets, CI still builds debug APKs but deliberately skips release signing.
