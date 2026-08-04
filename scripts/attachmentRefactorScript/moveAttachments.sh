#!/bin/bash

#working directory
cd /project/epic/attachments

#temp directory for copied files with new name
mkdir /project/epic/tempAttachmentDir

TMP=""

find . -type f -name '*.*' -print0 | 
while IFS= read -r -d '' file; do
   
#remove leading '.' from the file listing
TMP="${file:1}"

#remove file separator from string to match database update script
TMP="${TMP///}"

#copy file to temp directory
cp "$file" "/project/epic/tempAttachmentDir/$TMP"
done

#change working directory to the temp dir
cd /project/epic/tempAttachmentDir

#copy all new files to the main attachment dir
cp -R * /project/epic/attachments
