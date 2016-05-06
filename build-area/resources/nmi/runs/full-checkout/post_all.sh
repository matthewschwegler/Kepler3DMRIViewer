#! /bin/sh
date=`date +%Y-%m-%e-%H.%M` 
echo "Starting POST_ALL"
echo -n "Working directory is: "
echo `pwd`

echo "Ubuntu 5.10 Errors" >> notify.nmi
echo "------------------" >> notify.nmi
cat ../nmi:x86_ubuntu_5.10/remote_task.err >> notify.nmi

echo "\n" >> notify.xml
echo "RHAS 4 Errors" >> notify.nmi
echo "-------------" >> notify.nmi
cat ../nmi:x86_rhas_4/remote_task.err >> notify.nmi

echo "\n" >> notify.xml
echo "Fedora Core 5 Errors" >> notify.nmi
echo "--------------------" >> notify.nmi
cat ../nmi:x86_fc_5/remote_task.err >> notify.nmi

echo "\n" >> notify.xml
echo "OSX 10.4 Errors" >> notify.nmi
echo "---------------" >> notify.nmi
cat ../nmi:x86_macos_10.4/remote_task.err >> notify.nmi

echo "\n" >> notify.xml
echo "OSX 10.5 Errors" >> notify.nmi
echo "---------------" >> notify.nmi
cat ../nmi:x86_64_macos_10.5/remote_task.err >> notify.nmi

echo "\n" >> notify.xml
echo "Winnt 5.1 Errors" >> notify.nmi
echo "---------------" >> notify.nmi
cat ../nmi:x86_winnt_5.1/remote_task.err >> notify.nmi

echo "\n" >> notify.xml
echo "Winnt 6.0 Errors" >> notify.nmi
echo "---------------" >> notify.nmi
cat ../nmi:x86_winnt_6.0/remote_task.err >> notify.nmi

tar -zcvf kepler-nightly-$date.tar.gz kepler
echo "POST_ALL completed"
exit
