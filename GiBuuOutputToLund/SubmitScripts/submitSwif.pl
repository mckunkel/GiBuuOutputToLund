#!/apps/bin/perl -w

use strict;
use warnings;

my $workflow  = "-workflow gibuu";
my $project   = "-project clas12";
my $track     = "-track simulation";
my $time      = "-time 800min";
my $OS        = "-os centos7";
my $shell     = "-shell /bin/tcsh";
my $ram       = "-ram 4g";
my $disk      = "-disk 6g";
my $CPU_count = "-cores 1";

#
#
################################

my $iJob = 0;

my $submit_dir       = "/work/clas/clasg12/mkunkel/GiBuu";
my $jobcardInput_dir = "$submit_dir/clas/jobCard";
my $lundOutput_dir   = "/volatile/clas12/mkunkel/EtaPrimeDilepton/LundFiles";
my $gibuuOutput_dir  = "/volatile/clas12/mkunkel/EtaPrimeDilepton/GiBUUFiles";

my $nJobs = 500;    # total number of jobs

my $command_source = "source clas12.cshrc";
my $command_exit   = "exit 0";

my $input_0 = "-input clas12.cshrc /u/home/mkunkel/CSHRC_FILES/clas12.cshrc";
my $input_1 =
"-input HiLepton_CLAS12_dilep.job $jobcardInput_dir/HiLepton_CLAS12_dilep.job";

while ( $iJob < $nJobs ) {

	#check to see in gemc file already exists
	my $gibuu_out = "$gibuuOutput_dir/Dilepton_FullEvents_" . $iJob . ".dat";
	my $mv_gibuu  = "-output Dilepton_FullEvents.dat $gibuu_out";

	my $lund_out = "$lundOutput_dir/Dilepton_FullEvents_" . $iJob . ".lund";
	my $mv_lund  = "-output Dilepton_FullEvents_1.lund $lund_out";

	if ( -e $gibuu_out ) {
		print "YO dumbass, are you overwriting an existing file? Tsk Tsk!";
		$iJob++;
		next;
	}
	my $makeJobcard =
	  "perl -p -i -e 's/SEED=45678/SEED=$iJob/g' HiLepton_CLAS12_dilep.job";
	my $gibuu_run =
	  "$submit_dir/release2016/testRun/GiBUU.x < HiLepton_CLAS12_dilep.job";
	my $gibuuToLund =
"/apps/scicomp/java/jdk1.8/bin/java -Xmx128m client.UseGiBuuToLund Dilepton_FullEvents.dat Dilepton_FullEvents.lund 1000";

	open my $command_file, ">command.dat"
	  or die "cannot open command.dat file:$!";
	print $command_file
	  "$command_source ; $makeJobcard; $gibuu_run ; $command_exit";
	close $command_file;

	my $sub =
"swif add-job $workflow $project $track $time $OS $ram $disk $CPU_count $input_0 $input_1 -script command.dat $mv_gibuu";
	system($sub);
	print "$sub \n\n";

	$iJob++;

}
