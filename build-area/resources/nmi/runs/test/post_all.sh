#! /bin/sh
set -v on
date=`date +%Y-%m-%d-%H-%M`
echo "Starting POST_ALL"
echo -n "Working directory is: "
echo `pwd`
echo ' files here:'
ls
echo "\n\n"

echo "Ubuntu 5.10 Errors" >> notify.nmi
echo "------------------" >> notify.nmi
cat ../x86_ubuntu_5.10/remote_task.err >> notify.nmi

echo "\n\n" >> notify.xml
echo "RHAS 4 Errors" >> notify.nmi
echo "-------------" >> notify.nmi
cat ../x86_rhas_4/remote_task.err >> notify.nmi

echo "\n\n" >> notify.xml
echo "Fedora Core 5 Errors" >> notify.nmi
echo "--------------------" >> notify.nmi
cat ../x86_fc_5/remote_task.err >> notify.nmi

echo "\n\n" >> notify.xml
echo "OSX 10.4 Errors" >> notify.nmi
echo "---------------" >> notify.nmi
cat ../x86_macos_10.4/remote_task.err >> notify.nmi

echo "\n\n" >> notify.xml
echo "OSX 10.5 Errors" >> notify.nmi
echo "---------------" >> notify.nmi
cat ../x86_64_macos_10.5/remote_task.err >> notify.nmi

echo "\n\n" >> notify.xml
echo "Winnt 5.1 Errors" >> notify.nmi
echo "---------------" >> notify.nmi
cat ../x86_winnt_5.1/remote_task.err >> notify.nmi

echo "\n\n" >> notify.xml
echo "Winnt 6.0 Errors" >> notify.nmi
echo "---------------" >> notify.nmi
cat ../x86_winnt_6.0/remote_task.err >> notify.nmi

echo "\n\nProcessing results.tar.gz file...\n"

echo "mkdir results"
mkdir results
echo "cp ../x86_macos_10.4/results.tar.gz ."
cp ../x86_macos_10.4/results.tar.gz .
echo "cd results"
cd results
echo "mv ../results.tar.gz ."
cp ../results.tar.gz .
echo "tar -zxvf results.tar.gz"
tar -zxvf results.tar.gz


echo "POST_ALL completed"
exit
