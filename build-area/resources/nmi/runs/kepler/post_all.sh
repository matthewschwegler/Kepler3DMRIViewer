#! /bin/sh
set -v on
date=`date +%Y-%m-%d-%H-%M`
echo -n "Working directory is: "
echo `pwd`
echo "Error stream for platform x86_macos_10.4" >> notify.nmi
echo "-------------------------------------------------------" >> notify.nmi
cat ../x86_macos_10.4/remote_task.err >>notify.nmi
echo "Output stream for platform x86_macos_10.4" >> notify.nmi
echo "-------------------------------------------------------" >> notify.nmi
cat ../x86_macos_10.4/remote_task.out >> notify.nmi
echo "-------------------------------------------------------" >> notify.nmi
echo "-------------------------------------------------------" >> notify.nmi
echo "Error stream for platform x86_fc_5" >> notify.nmi
echo "-------------------------------------------------------" >> notify.nmi
cat ../nmi:x86_fc_5/remote_task.err >> notify.nmi
echo "Output stream for platform x86_fc_5" >> notify.nmi
echo "-------------------------------------------------------" >> notify.nmi
cat ../nmi:x86_fc_5/remote_task.out >> notify.nmi
mkdir results
cp ../nmi:x86_fc_5/results.tar.gz .
cd results
cp ../results.tar.gz .
tar -zxvf results.tar.gz
rm results.tar.gz
echo "POST_ALL completed"
exit
