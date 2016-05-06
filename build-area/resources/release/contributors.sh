#!/bin/sh
#
# A shell script to get the current list of contributors from SVN.
# Matt Jones
# 20 November 2008

# First dump all commits to the kepler module into a file
svn -q log https://code.kepler-project.org/code/kepler | grep -v "^---" > all-commits.log
svn -q log https://code.kepler-project.org/code/kepler-docs | grep -v "^---" >> all-commits.log

# Next sort the file by committer and date
cat all-commits.log | grep -v "(no author)" |awk 'BEGIN {OFS="|"} {print $3, $5, $6, $1}' |sort > all-commits-sorted.log

# Next find the first commit for each user and output it, along with the date
# and time
cat all-commits-sorted.log |awk -F\| 'BEGIN {OFS="|"; currentname=" "} {if (currentname !~ $1) {currentname = $1; print $2, $3, $1} }'|sort | awk -F\| 'BEGIN {OFS="|"} {print $3, $1, $2}' > contributors.txt
echo "contributors.txt created."

# Output an HTML version of the contributors file
cat contributors.txt | awk -F\| '{print "<tr><td>" $1 "</td><td>" $2 "</td></tr>"}' > contributors.html
echo "contributors.html created."

# Clean up our temporary files
rm all-commits.log
rm all-commits-sorted.log
