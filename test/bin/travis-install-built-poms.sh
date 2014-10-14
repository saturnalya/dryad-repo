#!/bin/bash
#
#set -euo pipefail
set -e

CURRENT_VERSION="1.7.3-SNAPSHOT";
BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
REPO_DIR="$( readlink -e $BIN_DIR/../../)";

echo BIN_DIR: $BIN_DIR
echo REPO_DIR: $REPO_DIR

function install_file_path() {
    FILE="$1"
    TYPE="$2"
    VERSION="$3"
    ARTIFACT=`basename $FILE | sed "s/-${VERSION}.${TYPE}//"`;
    GROUP=$(jar tf $FILE | grep "META-INF/maven/" | grep -v "$ARTIFACT" | grep -v pom  | sed 's/META-INF\/maven//g' | perl -pe 's:[/\s]+::g');
    install "$FILE" "$ARTIFACT" "$GROUP" "$TYPE" "$VERSION" "" 
}

function install() {
    FILE="$1"
    ARTIFACT="$2"
    GROUP="$3"
    TYPE="$4"
    VERSION="$5"
    OTHER="$6"
    echo FILE: $FILE
    echo Installing $GROUP $ARTIFACT
    mvn install:install-file            \
        -DgroupId="$GROUP"              \
        -DartifactId="$ARTIFACT"        \
        -Dversion="$VERSION"            \
        -Dpackaging="$TYPE"             \
        $OTHER                          \
        -Dfile="$FILE"
    echo Success for mvn: $?
}


# install from dryad-repo dirs starting dspace-*
for f in `ls "$REPO_DIR" | grep "dspace-"`;
do
    for g in `find "$f" -name "*${CURRENT_VERSION}.jar" | grep -v lib`;
    do
        install_file_path "$g" "jar" "$CURRENT_VERSION";
    done;
    for g in `find "$f" -name "*${CURRENT_VERSION}.war" | grep -v lib`;
    do
        install_file_path "$g" "war" "$CURRENT_VERSION";
    done;
done;

# install from dryad-repo/dspace/modules dirs
for f in `find "$REPO_DIR/dspace/modules" -name "*${CURRENT_VERSION}.jar" | grep -v lib`;
do
    install_file_path "$f" "jar" "$CURRENT_VERSION";
done;

# odds and ends
install "$REPO_DIR/dspace/modules/api/target/api-${CURRENT_VERSION}-tests.jar" "api" "org.dspace.modules" "test-jar" "$CURRENT_VERSION"
install "$REPO_DIR/dspace/modules/bagit/dspace-bagit-api/target/bagit-api-0.0.1.jar" "bagit-api" "org.dspace.modules" "jar" "0.0.1"
install "$REPO_DIR/dspace/modules/versioning/versioning-webapp/target/versioning-webapp-${CURRENT_VERSION}-classes.jar" \
    "versioning-webapp" "org.dspace.modules" "jar" "$CURRENT_VERSION" "-Dclassifier=classes"
install "$REPO_DIR/dspace/modules/payment-system/payment-webapp/target/payment-webapp-${CURRENT_VERSION}-classes.jar" \
    "payment-webapp" "org.dspace.modules" "jar" "$CURRENT_VERSION" "-Dclassifier=classes"

