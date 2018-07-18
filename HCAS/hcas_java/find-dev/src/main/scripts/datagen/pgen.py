############################################
############################################
##Modules##
############################################
import string
import re
import os
import time
import codecs
import xml.sax.saxutils as xmlutils
import urllib
import shutil
import sys
import glob
import copy
import random
from multiprocessing import Pool, Manager
from random import choice
from itertools import izip,repeat
from lxml import etree as ETREE
from collections import defaultdict
from collections import Counter
from datetime import datetime
from datetime import timedelta

#Module to handle xml structures
#Warning! The ET module is not secure against maliciously constructed data.
#If you need to parse untrusted or unauthenticated data
#see http://docs.python.org/2/library/xml.html#xml-vulnerabilities.
import xml.etree.ElementTree as ET

import autnhealthcare.Healthcare as HC

# Default Configuration Parameters for tag.py script
# --------------------------------------------------
buildVersion='b000001'

# Production Configuration Settings
# -----------------------------------
bMultiThread=False
bmoveCompleted=False
doctype='xml'
file_ext='.xml'
basepath='D:/Healthcare/pgen/'
inbase='inxml_all'
dirs=['admissions']
compdir='complete'
outdir='outxml'
logdir='logs'
record_seperator='ADMISSION'
subjects_out=1000
start_date="2011-01-01 00:00:00"
end_date="2013-12-31 23:59:00"
text_file='sentences_d.txt'
num_caregivers=10
avg_note_lines=10
patients_per_file=10

batch_poolsize=8


# ----------------------------------------------
class hash(dict):
    def __getitem__(self, item):
        try:
            return dict.__getitem__(self, item)
        except KeyError:
            value = self[item] = type(self)()
            return value


def split2(p,s):
	return [x for x in re.split(p,s) if (not (x==''))]


def strTimeProp(start, end, format, prop):
    """Get a time at a proportion of a range of two formatted times.

    start and end should be strings specifying times formated in the
    given format (strftime-style), giving an interval [start, end].
    prop specifies how a proportion of the interval to be taken after
    start.  The returned time will be in the specified format.
    """

    stime = time.mktime(time.strptime(start, format))
    etime = time.mktime(time.strptime(end, format))

    ptime = stime + prop * (etime - stime)

    return time.strftime(format, time.localtime(ptime))

# '%m/%d/%Y %I:%M %p'
def randomDate(start, end, prop):
    return strTimeProp(start, end, '%Y-%m-%d %H:%M:%S', prop)

def splitsentence(paragraph):
    sentenceEnders = re.compile('[.]')
    sentenceList = sentenceEnders.split(paragraph)
    return sentenceList

if __name__ == '__main__':

	
	def chunk(l,n):
		return [l[i:i+n] for i in xrange(0,len(l),n)]
		
	
	# Process XML file containing docuuments to tag
	def processXMLFile(filename, indir, outdir, compdir):
		global h_sex
		global h_age
		global h_age_vals
		global h_sex_vals
		global h_days
		global h_days_vals
		global hist
		global hist_cnt
		global drg_picd9map
		global text_map
		global text_list
		
		stime = time.time()
		ltime=stime;
		errorlist=[]
		perfmon=[]
	
	# try:
		file_path = os.path.join(indir, filename)
		print "Started ("+file_path+")"
		tree = ETREE.parse(file_path,parser)
		root = tree.getroot()
		out_path=os.path.join(outdir, filename)
		comp_path=os.path.join(compdir, filename)

		records=root.findall('.//'+record_seperator)
		print "\nprocessing "+str(len(records))+" admissions"
		for r in records:
			# get cohort sex | age [<1,<5,<12,<18,<40,<60,<80,80+] | ICD9 (Primary) 
			sex='NONE'
			agegrp='NONE'
			age='NONE'

			n=r.find('PATIENT/SEX')
			if n is not None:
				sex=n.text
			if (sex) in h_sex:
				h_sex[sex]+=1
			else:
				h_sex[sex]=1
			h_sex_vals+=1
				
			n=r.find('PATIENT/AGE_AT_ADMISSION_GRP')
			if n is not None:
				agegrp=n.text
					
			n=r.find('PATIENT/AGE_AT_ADMISSION')
			if n is not None:
				age=n.text
				if int(age)>100:
					age='100'
			if (age) in h_age:
				h_age[age]+=1
			else:
				h_age[age]=1
			h_age_vals+=1

			n=r.find('PATIENT/ADMISSION_DATE')
			if n is not None:
				admit_dt=n.text
				admit_dt2=datetime.strptime(str(admit_dt),'%Y-%m-%d %H:%M:%S')
			n=r.find('PATIENT/DISCHARGE_DATE')
			if n is not None:
				disch_dt=n.text
				disch_dt2=datetime.strptime(str(disch_dt),'%Y-%m-%d %H:%M:%S')
			
			adm_days=disch_dt2-admit_dt2
			adm_days_int=round(adm_days.days+0.5,0)
			if h_days[adm_days]:
				h_days[adm_days]+=1
			else:
				h_days[adm_days]=1
			h_days_vals+=1
			
			# build histogram per patient
			pkeys=[
				'PATIENT/HOSPITAL_EXPIRE',
				'PATIENT/ETHNICITY',
				'PATIENT/ADMISSION_TYPE',
				'PATIENT/RELIGION',
				'PATIENT/ADMISSION_SOURCE',
				'PATIENT/OVERALL_PAYOR_GROUP',
				'PATIENT/MARITAL_STATUS',
			]
			for k in pkeys:	
				node=r.find(k)
				if node is not None:
					value=node.text
					if hist[sex][agegrp][k][value]:
						hist[sex][agegrp][k][value]+=1
					else:
						hist[sex][agegrp][k][value]=1
				
				if hist_cnt[sex][agegrp][k]:
					hist_cnt[sex][agegrp][k]+=1
				else:
					hist_cnt[sex][agegrp][k]=1

			
			nodes=r.findall('ICD9')
			#	'ICD9CNT' 
			icd9cnt=len(nodes)
			if hist[sex][agegrp]['ICD9CNT'][icd9cnt]:
				hist[sex][agegrp]['ICD9CNT'][icd9cnt]+=1
			else:
				hist[sex][agegrp]['ICD9CNT'][icd9cnt]=1
			if hist_cnt[sex][agegrp]['ICD9CNT']:
				hist_cnt[sex][agegrp]['ICD9CNT']+=1
			else:
				hist_cnt[sex][agegrp]['ICD9CNT']=1
			
			
			#	'ICD9' List
			for n in nodes:
				value=n.text
				if hist[sex][agegrp]['ICD9'][value]:
					hist[sex][agegrp]['ICD9'][value]+=1
				else:
					hist[sex][agegrp]['ICD9'][value]=1
			if hist_cnt[sex][agegrp]['ICD9']:
				hist_cnt[sex][agegrp]['ICD9']+=1
			else:
				hist_cnt[sex][agegrp]['ICD9']=1
			
			
			# Adding DRG Code to histogram and associated primary ICD9 code
			node=r.find('DRG')
			picd9val=''
			drgval=''
			if node is not None:
				drgvalue=ETREE.tostring(node)
				picd9=r.find('ICD9')
				if picd9 is not None:
					picd9val=picd9.text

			if hist[sex][agegrp]['DRG'][drgvalue]:
				hist[sex][agegrp]['DRG'][drgvalue]+=1
			else:
				hist[sex][agegrp]['DRG'][drgvalue]=1				
				drg_picd9map[drgvalue]=picd9val
			if hist_cnt[sex][agegrp]['DRG']:
				hist_cnt[sex][agegrp]['DRG']+=1
			else:
				hist_cnt[sex][agegrp]['DRG']=1
					
			# ADMISSION/ICU/ICU/MEDEVENTS							
			# ADMISSION/ICU/ICU/MEDEVENTS/EVENT								
			# ADMISSION/ICU/ICU/MEDEVENTS/EVENT/NAME			
			# ADMISSION/ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES
			nodes=r.findall('./ICU/ICU/MEDEVENTS/EVENT')

			medcnt=len(nodes)
			if hist[sex][agegrp]['MEDCNT'][medcnt]:
				hist[sex][agegrp]['MEDCNT'][medcnt]+=1
			else:
				hist[sex][agegrp]['MEDCNT'][medcnt]=1
			
			if hist_cnt[sex][agegrp]['MEDCNT']:
				hist_cnt[sex][agegrp]['MEDCNT']+=1
			else:
				hist_cnt[sex][agegrp]['MEDCNT']=1


			for n in nodes:
				if n is not None:
					value=n.find('NAME').text
					occur=float(n.find('OCCURRENCES').text)/adm_days_int
					if hist[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/NAME'][value]:
						hist[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/NAME'][value]+=1
					else:
						hist[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/NAME'][value]=1			
				
					if hist_cnt[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/NAME']:
						hist_cnt[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/NAME']+=1
					else:
						hist_cnt[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/NAME']=1

					if hist[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES'][value][occur]:
						hist[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES'][value][occur]+=1
					else:
						hist[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES'][value][occur]=1

					if hist_cnt[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES'][value]:
						hist_cnt[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES'][value]+=1
					else:
						hist_cnt[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES'][value]=1

			# ADMISSION/ICU/ICU/FIRST_CAREUNIT					
			nodes=r.findall('./ICU/ICU/FIRST_CAREUNIT')
			
			for n in nodes:
				if n is not None:
					value=n.text
					if hist[sex][agegrp]['ICU/ICU/FIRST_CAREUNIT'][value]:
						hist[sex][agegrp]['ICU/ICU/FIRST_CAREUNIT'][value]+=1
					else:
						hist[sex][agegrp]['ICU/ICU/FIRST_CAREUNIT'][value]=1
			
					if hist_cnt[sex][agegrp]['ICU/ICU/FIRST_CAREUNIT']:
						hist_cnt[sex][agegrp]['ICU/ICU/FIRST_CAREUNIT']+=1
					else:
						hist_cnt[sex][agegrp]['ICU/ICU/FIRST_CAREUNIT']=1
						
			# ADMISSION/POEEVENTS
			# ADMISSION/POEEVENTS/EVENT
			# ADMISSION/POEEVENTS/EVENT/NAME
			# ADMISSION/POEEVENTS/EVENT/OCCURANCES
			
			nodes=r.findall('./POEEVENTS/EVENT')

			poecnt=len(nodes)
			if hist[sex][agegrp]['POECNT'][poecnt]:
				hist[sex][agegrp]['POECNT'][poecnt]+=1
			else:
				hist[sex][agegrp]['POECNT'][poecnt]=1
			
			if hist_cnt[sex][agegrp]['POECNT']:
				hist_cnt[sex][agegrp]['POECNT']+=1
			else:
				hist_cnt[sex][agegrp]['POECNT']=1


			for n in nodes:
				if n is not None:
					value=n.find('NAME').text
					occur=float(n.find('OCCURRENCES').text)/adm_days_int
					if hist[sex][agegrp]['POEEVENTS/EVENT/NAME'][value]:
						hist[sex][agegrp]['POEEVENTS/EVENT/NAME'][value]+=1
					else:
						hist[sex][agegrp]['POEEVENTS/EVENT/NAME'][value]=1			
				
					if hist_cnt[sex][agegrp]['POEEVENTS/EVENT/NAME']:
						hist_cnt[sex][agegrp]['POEEVENTS/EVENT/NAME']+=1
					else:
						hist_cnt[sex][agegrp]['POEEVENTS/EVENT/NAME']=1

					if hist[sex][agegrp]['POEEVENTS/EVENT/OCCURRENCES'][value][occur]:
						hist[sex][agegrp]['POEEVENTS/EVENT/OCCURRENCES'][value][occur]+=1
					else:
						hist[sex][agegrp]['POEEVENTS/EVENT/OCCURRENCES'][value][occur]=1

					if hist_cnt[sex][agegrp]['POEEVENTS/EVENT/OCCURRENCES'][value]:
						hist_cnt[sex][agegrp]['POEEVENTS/EVENT/OCCURRENCES'][value]+=1
					else:
						hist_cnt[sex][agegrp]['POEEVENTS/EVENT/OCCURRENCES'][value]=1
				
			
			# ADMISSION/LABEVENTS
			# ADMISSION/LABEVENTS/EVENT
			# ADMISSION/LABEVENTS/EVENT/NAME
			# ADMISSION/LABEVENTS/EVENT/OCCURANCES

			nodes=r.findall('./LABEVENTS/EVENT')

			labcnt=len(nodes)
			if hist[sex][agegrp]['LABCNT'][labcnt]:
				hist[sex][agegrp]['LABCNT'][labcnt]+=1
			else:
				hist[sex][agegrp]['LABCNT'][labcnt]=1
			
			if hist_cnt[sex][agegrp]['LABCNT']:
				hist_cnt[sex][agegrp]['LABCNT']+=1
			else:
				hist_cnt[sex][agegrp]['LABCNT']=1


			for n in nodes:
				if n is not None:
					value=n.find('NAME').text
					occur=float(n.find('OCCURRENCES').text)/adm_days_int
					if hist[sex][agegrp]['LABEVENTS/EVENT/NAME'][value]:
						hist[sex][agegrp]['LABEVENTS/EVENT/NAME'][value]+=1
					else:
						hist[sex][agegrp]['LABEVENTS/EVENT/NAME'][value]=1			
				
					if hist_cnt[sex][agegrp]['LABEVENTS/EVENT/NAME']:
						hist_cnt[sex][agegrp]['LABEVENTS/EVENT/NAME']+=1
					else:
						hist_cnt[sex][agegrp]['LABEVENTS/EVENT/NAME']=1

					if hist[sex][agegrp]['LABEVENTS/EVENT/OCCURRENCES'][value][occur]:
						hist[sex][agegrp]['LABEVENTS/EVENT/OCCURRENCES'][value][occur]+=1
					else:
						hist[sex][agegrp]['LABEVENTS/EVENT/OCCURRENCES'][value][occur]=1

					if hist_cnt[sex][agegrp]['LABEVENTS/EVENT/OCCURRENCES'][value]:
						hist_cnt[sex][agegrp]['LABEVENTS/EVENT/OCCURRENCES'][value]+=1
					else:
						hist_cnt[sex][agegrp]['LABEVENTS/EVENT/OCCURRENCES'][value]=1



			# ADMISSION/ICU
			# ADMISSION/ICU/COUNT
			# ADMISSION/ICU/ICU/ICUSTAY_ID
			# ADMISSION/ICU/ICU/FIRST_CAREUNIT					
			# ADMISSION/ICU/ICU/LAST_CAREUNIT					^
			# ADMISSION/ICU/ICU/INTIME										*
			# ADMISSION/ICU/ICU/OUTTIME										*
			# ADMISSION/ICU/ICU/LOS								- Derived from intime/outtime
			# ADMISSION/ICU/ICU/LOS_GRP							^
			# ADMISSION/ICU/ICU/CENSUS_EVENT					^
			# ADMISSION/ICU/ICU/CENSUS_EVENT/CENSUS_ID						^
			# ADMISSION/ICU/ICU/CENSUS_EVENT/INTIME							^
			# ADMISSION/ICU/ICU/CENSUS_EVENT/OUTTIME							^
			# ADMISSION/ICU/ICU/CENSUS_EVENT/CAREUNIT								
			# ADMISSION/ICU/ICU/CENSUS_EVENT/DESTINATION								
			# ADMISSION/ICU/ICU/CENSUS_EVENT/DISCHARGE_STATUS								
			# ADMISSION/ICU/ICU/CENSUS_EVENT/COUNT				- Sequence #

			# ADMISSION/NOTEEVENT
			# ADMISSION/NOTEEVENT/CHART_TIME									*
			
			nodes=r.findall('./NOTEEVENT')
			for n in nodes:
				if n is not None:
					# ADMISSION/NOTEEVENT/CAREUNIT
					value=n.find('CAREUNIT').text
					if hist[sex][agegrp]['NOTEEVENT/CAREUNIT'][value]:
						hist[sex][agegrp]['NOTEEVENT/CAREUNIT'][value]+=1
					else:
						hist[sex][agegrp]['NOTEEVENT/CAREUNIT'][value]=1			
				
					if hist_cnt[sex][agegrp]['NOTEEVENT/CAREUNIT']:
						hist_cnt[sex][agegrp]['NOTEEVENT/CAREUNIT']+=1
					else:
						hist_cnt[sex][agegrp]['NOTEEVENT/CAREUNIT']=1

					# ADMISSION/NOTEEVENT/CAREGIVER
					value=n.find('CAREGIVER').text
					if hist[sex][agegrp]['NOTEEVENT/CAREGIVER'][value]:
						hist[sex][agegrp]['NOTEEVENT/CAREGIVER'][value]+=1
					else:
						hist[sex][agegrp]['NOTEEVENT/CAREGIVER'][value]=1			
				
					if hist_cnt[sex][agegrp]['NOTEEVENT/CAREGIVER']:
						hist_cnt[sex][agegrp]['NOTEEVENT/CAREGIVER']+=1
					else:
						hist_cnt[sex][agegrp]['NOTEEVENT/CAREGIVER']=1

					# ADMISSION/NOTEEVENT/TITLE
					value=n.find('TITLE').text
					if hist[sex][agegrp]['NOTEEVENT/TITLE'][value]:
						hist[sex][agegrp]['NOTEEVENT/TITLE'][value]+=1
					else:
						hist[sex][agegrp]['NOTEEVENT/TITLE'][value]=1			
				
					if hist_cnt[sex][agegrp]['NOTEEVENT/TITLE']:
						hist_cnt[sex][agegrp]['NOTEEVENT/TITLE']+=1
					else:
						hist_cnt[sex][agegrp]['NOTEEVENT/TITLE']=1
				
					# ADMISSION/NOTEEVENT/CATEGORY
					value=n.find('CATEGORY').text
					if hist[sex][agegrp]['NOTEEVENT/CATEGORY'][value]:
						hist[sex][agegrp]['NOTEEVENT/CATEGORY'][value]+=1
					else:
						hist[sex][agegrp]['NOTEEVENT/CATEGORY'][value]=1			
				
					if hist_cnt[sex][agegrp]['NOTEEVENT/CATEGORY']:
						hist_cnt[sex][agegrp]['NOTEEVENT/CATEGORY']+=1
					else:
						hist_cnt[sex][agegrp]['NOTEEVENT/CATEGORY']=1
			
		
					# ADMISSION/NOTEEVENT/TEXT
					value=n.find('TEXT').text
					textlist=splitsentence(value)
					for t1 in textlist:
						# trim spaces
						t1=t1.strip()
						# remove newlines
						t1=t1.strip('\n')
						#if len(t1)>10:
						#	if t1 not in text_map[picd9val]:
							#	text_map[picd9val][t1]=1
										
		# print str(records)
		# tree.write(out_path,encoding="UTF-8",xml_declaration=True)
		ctime = time.time()
		if (bmoveCompleted):
			shutil.move(file_path,comp_path)
		print "Completed ("+out_path+") Time taken = "+str(round(ctime-stime,2))+" seconds"
		sys.stdout.flush()
		#ltime=ctime
	# except ETREE.ParseError, e:
	# 	error = (-1,'Exception',e)
	# 	print filename + "erred out",
	#	print e
	#	errorlist.append([-1,'exception',e])

	#	for sex, details in hist['sex'].iteritems():
	#		print sex,'-->', details

		
		
		
		
		return errorlist,perfmon
	def sample(dict,dict_cnt):
		if type(dict_cnt) is int:
			n=int(random.random()*dict_cnt)
		else:
			return 'NONE'
		cnt=0
		for k in dict:		
			cnt+=dict[k]
			if (cnt>n):
				return k
				break;
		return 'NONE'
	
		
		
# Main Function
	indirs=[]
	outdirs=[]
	ctable={}
	ptable={}
	h_sex=hash()
	h_age=hash()
	h_days=hash()
	h_age_vals=0
	h_sex_vals=0
	h_days_vals=0
	hist=hash()
	hist_cnt=hash()
	drg_picd9map=hash()
	text_map=hash()
	dicttree=None
	croot=None
	pool=Pool(batch_poolsize)
	parser = ETREE.XMLParser(strip_cdata=False)

	print 'Number of arguments:', len(sys.argv), 'arguments.'
	print 'Argument List:', str(sys.argv)
	
	stime = time.time()
	print 'Start UTC: '+str(stime)

	with open(text_file) as f:
		text_list = f.read().splitlines()
	# print text_list


	for subdir in dirs:
		inpath=basepath+'/'+inbase+'/'+subdir
		outpath=basepath+'/'+outdir+'/'+subdir
		comppath=basepath+'/'+compdir+'/'+subdir
		if (not os.path.isdir(outpath)):
			os.makedirs(outpath)
		if (not os.path.isdir(comppath)):
			os.makedirs(comppath)

		# loop thru the files in the directory
		xmlFiles = []
		for dir_entry in os.listdir(inpath):
			if dir_entry.endswith(file_ext):
				xmlFiles.append(dir_entry)

		for xmlFile in xmlFiles:
			args = xmlFile, inpath, outpath
			print "Processing:"+xmlFile
			errlist,perfom=processXMLFile(xmlFile,inpath,outpath,comppath)

			
	admissionsdir=outdir+'/admissions/'
	lastfilenum=-1

	# clear out xml out of directories
	if (not os.path.isdir(admissionsdir)):
		os.makedirs(admissionsdir)
			
	# Generate Population
	for j in range(subjects_out):
	
		filenum=int(j/patients_per_file)
		if (lastfilenum != filenum):
			if (lastfilenum>=0):
				fa.write("\n</AdmissionList>");
				fa.close()
			admissionfname=admissionsdir+'/admissions-'+str(filenum)+'.xml'
			fa=codecs.open(admissionfname,encoding='utf-8',mode='w+')
			fa.write("<?xml version='1.0' encoding='UTF-8'?>\n");
			fa.write("<AdmissionList>\n");
		lastfilenum=filenum

		doc=ETREE.Element('DOCUMENT')		
		admission=ETREE.SubElement(doc,'ADMISSION')		
		
		print "-------------------------"
		# ^ ADMISSION/HADM_ID
		hadm_id=j
		print 'hadm_id:',hadm_id
		hadm=ETREE.SubElement(admission,'HADM_ID')
		hadm.text=str(hadm_id)
		
		patient=ETREE.SubElement(admission,'PATIENT')
	
	# ^ ADMISSION/PATIENT/SUBJECT_ID					- Sequential
		subject_id=int(random.random()*1000000)
		print 'subject_id:',subject_id
		sub=ETREE.SubElement(patient,'SUBJECT_ID')
		sub.text=str(subject_id)
				
		sex=sample(h_sex,h_sex_vals)
		print 'sex:',sex
		sub=ETREE.SubElement(patient,'SEX')
		sub.text=str(sex)
		
		age=int(sample(h_age,h_age_vals))
		print 'age',age
		sub=ETREE.SubElement(patient,'AGE_AT_ADMISSION')
		sub.text=str(age)

		agegrp=''
		if (age<1):
			agegrp=' <01'
		elif ((age>=1) and (age<5)):
			agegrp='01-04'
		elif ((age>=5) and (age<10)):
			agegrp='05-09'
		elif ((age>=10) and (age<18)):
			agegrp='10-17'
		elif ((age>=18) and (age<35)):
			agegrp='18-35'
		elif ((age>=35) and (age<65)):
			agegrp='35-64'
		elif (age>=65) :
			agegrp='65+'
		print 'agegrp',agegrp
		sub=ETREE.SubElement(patient,'AGE_AT_ADMISSION_GRP')
		sub.text=str(agegrp)

		
		# ^ ADMISSION/PATIENT/ADMISSION_DATE					
		admit_dt = randomDate(start_date,end_date,random.random())
		admit_dt2=datetime.strptime(str(admit_dt),'%Y-%m-%d %H:%M:%S')
		print 'admit_dt',admit_dt
		sub=ETREE.SubElement(patient,'ADMISSION_DATE')
		sub.text=str(admit_dt)
		
		# ^ ADMISSION/PATIENT/ADMISSION_DATE_YRMON			
		admit_dt_yrmon=admit_dt2.strftime('%Y_%m')
		sub=ETREE.SubElement(patient,'ADMISSION_DATE_YRMON')
		sub.text=str(admit_dt_yrmon)

		# ^ ADMISSION/PATIENT/ADMISSION_DATE_MON_GRP			
		admit_dt_mon_grp=admit_dt2.strftime('%m')
		sub=ETREE.SubElement(patient,'ADMISSION_DATE_MON_GRP')
		sub.text=str(admit_dt_mon_grp)

		# ^ ADMISSION/PATIENT/ADMISSION_DATE_DAYMON_GRP
		admit_dt_daymon_grp=admit_dt2.strftime('%d')
		sub=ETREE.SubElement(patient,'ADMISSION_DATE_DAYMON_GRP')
		sub.text=str(admit_dt_daymon_grp)
		
		# ^ ADMISSION/PATIENT/ADMISSION_DATE_DAYWEEK_GRP
		admit_dt_dayweek_grp=admit_dt2.strftime('%w-%a')
		sub=ETREE.SubElement(patient,'ADMISSION_DATE_DAYWEEK_GRP')
		sub.text=str(admit_dt_dayweek_grp)

		# ^ ADMISSION/PATIENT/ADMISSION_DATE_WEEK_GRP		
		admit_dt_week_grp=admit_dt2.strftime('%U')
		sub=ETREE.SubElement(patient,'ADMISSION_DATE_WEEK_GRP')
		sub.text=str(admit_dt_week_grp)

		# ADMISSION/PATIENT/DISCHARGE_DATE	
		td=timedelta(hours=int(random.random()*12))
		admdays=sample(h_days,h_days_vals)-td
		admdays_int=int(round(admdays.days+0.5,0))
		# delta=timedelta(int(admdays))
		# disch_dt=((datetime.combine(datetime.date(1,1,1),admit_dt2) + delta).time())
		disch_dt=admit_dt2+admdays
		print 'disch_dt',disch_dt
		sub=ETREE.SubElement(patient,'DISCHARGE_DATE')
		sub.text=str(disch_dt)
			
		# ADMISSION/PATIENT/DOB								*- Derived - Based Age_at_Admission			
		daysdelta=age*365
		if (age>0):
			daysdelta+=int(random.random()*160)
			
		tdb=timedelta(days=daysdelta)
		dob=admit_dt2-tdb
		dobstr=dob.strftime('%Y-%m-%d')
		print 'dob',dobstr
		sub=ETREE.SubElement(patient,'DOB')
		sub.text=str(dobstr)
		
		# ADMISSION/PATIENT/HOSPITAL_EXPIRE   				
		hospital_expire=sample(hist[sex][agegrp]['PATIENT/HOSPITAL_EXPIRE'],hist_cnt[sex][agegrp]['PATIENT/HOSPITAL_EXPIRE'])
		print 'hospital_expire',hospital_expire
		sub=ETREE.SubElement(patient,'HOSPITAL_EXPIRE')
		sub.text=str(hospital_expire)
		
		# ADMISSION/PATIENT/ETHNICITY
		ethnicity=sample(hist[sex][agegrp]['PATIENT/ETHNICITY'],hist_cnt[sex][agegrp]['PATIENT/ETHNICITY'])
		print 'ethnicity',ethnicity
		sub=ETREE.SubElement(patient,'ETHNICITY')
		sub.text=str(ethnicity)

		
		# ADMISSION/PATIENT/ADMISSION_TYPE
		admission_type=sample(hist[sex][agegrp]['PATIENT/ADMISSION_TYPE'],hist_cnt[sex][agegrp]['PATIENT/ADMISSION_TYPE'])
		print 'admission_type',admission_type
		sub=ETREE.SubElement(patient,'ADMISSION_TYPE')
		sub.text=str(admission_type)

		# ADMISSION/PATIENT/RELIGION
		religion=sample(hist[sex][agegrp]['PATIENT/RELIGION'],hist_cnt[sex][agegrp]['PATIENT/RELIGION'])
		print 'religion',religion
		sub=ETREE.SubElement(patient,'RELIGION')
		sub.text=str(religion)


		# ADMISSION/PATIENT/ADMISSION_SOURCE
		admission_source=sample(hist[sex][agegrp]['PATIENT/ADMISSION_SOURCE'],hist_cnt[sex][agegrp]['PATIENT/ADMISSION_SOURCE'])
		print 'admission_source',admission_source
		sub=ETREE.SubElement(patient,'ADMISSION_SOURCE')
		sub.text=str(admission_source)
		
		# ADMISSION/PATIENT/OVERALL_PAYOR_GROUP
		overall_payor_group=sample(hist[sex][agegrp]['PATIENT/OVERALL_PAYOR_GROUP'],hist_cnt[sex][agegrp]['PATIENT/OVERALL_PAYOR_GROUP'])
		print 'overall_payor_group',overall_payor_group
		sub=ETREE.SubElement(patient,'OVERALL_PAYOR_GROUP')
		sub.text=str(overall_payor_group)

		# ADMISSION/PATIENT/MARITAL_STATUS
		marital_status=sample(hist[sex][agegrp]['PATIENT/MARITAL_STATUS'],hist_cnt[sex][agegrp]['PATIENT/MARITAL_STATUS'])
		print 'marital_status',marital_status
		sub=ETREE.SubElement(patient,'MARITAL_STATUS')
		sub.text=str(marital_status)

		# ADMISSION/PATIENT/DOD								
		# ADMISSION/PATIENT/AGE_AT_DEATH
		# ADMISSION/PATIENT/AGE_AT_DEATH_GRP
		if (hospital_expire=='N'):
			dod='None'
			age_at_death='(N/A)'
			age_at_death_grp='(N/A)'
		else:
			dod=disch_dt
			age_at_death=int((disch_dt-dob).days/365)
			age_at_death_grp=''
			if (age_at_death<1):
				age_at_death_grp=' <01'
			elif ((age_at_death>=1) and (age_at_death<5)):
				age_at_death_grp='01-04'
			elif ((age_at_death>=5) and (age_at_death<10)):
				age_at_death_grp='05-09'
			elif ((age_at_death>=10) and (age_at_death<18)):
				age_at_death_grp='10-17'
			elif ((age_at_death>=18) and (age_at_death<35)):
				age_at_death_grp='18-35'
			elif ((age_at_death>=35) and (age_at_death<65)):
				age_at_death_grp='35-64'
			elif (age_at_death>=65) :
				age_at_death_grp='65+'
		
		
		print 'dod',dod
		print 'age_at_death',age_at_death
		print 'age_at_death_grp',age_at_death_grp
		sub=ETREE.SubElement(patient,'DOD')
		sub.text=str(dod)
		sub=ETREE.SubElement(patient,'AGE_AT_DEATH')
		sub.text=str(age_at_death)
		sub=ETREE.SubElement(patient,'AGE_AT_DEATH_GRP')
		sub.text=str(age_at_death_grp)
					
			
		# ADMISSION/DRG									
		# ADMISSION/DRG/CODE
		# ADMISSION/DRG/ITEMID
		# ADMISSION/DRG/DESC
		# ADMISSION/DRG/COST_WEIGHT
		# ADMISSION/DRG/DISPLAY
		drg=sample(hist[sex][agegrp]['DRG'],hist_cnt[sex][agegrp]['DRG'])
		print 'drg',drg
		ndrg=ETREE.fromstring(drg)
		admission.append(ndrg)
			
		# ADMISSION/ICD9								--> Drugs, Procedures
		picd9=drg_picd9map[drg]	
		icd9list=[picd9]
		icd9cnt=sample(hist[sex][agegrp]['ICD9CNT'],hist_cnt[sex][agegrp]['ICD9CNT'])
		print 'icd9cnt',icd9cnt
		for q in range(int(icd9cnt)):
			icd9=sample(hist[sex][agegrp]['ICD9'],hist_cnt[sex][agegrp]['ICD9'])
			if not icd9 in icd9list:
				icd9list.append(icd9)
				nicd9=ETREE.SubElement(admission,'ICD9')
				nicd9.text=str(icd9)
		print 'icd9',icd9list
		

		# ADMISSION/ICU
		nicu1=ETREE.SubElement(admission,'ICU')
		# ADMISSION/ICU/COUNT
		icucnt=ETREE.SubElement(nicu1,'COUNT')
		icucnt.text='1'

		nicu2=ETREE.SubElement(nicu1,'ICU')

		# ADMISSION/ICU/ICU/ICUSTAY_ID
		icuid=ETREE.SubElement(nicu2,'ICUSTAY_ID')
		icuid.text=str(int(random.random()*1000000))
		
		# ADMISSION/ICU/ICU/FIRST_CAREUNIT	
		fcu=sample(hist[sex][agegrp]['ICU/ICU/FIRST_CAREUNIT'],hist_cnt[sex][agegrp]['ICU/ICU/FIRST_CAREUNIT'])		
		icufcu=ETREE.SubElement(nicu2,'FIRST_CAREUNIT')
		icufcu.text=fcu
		
		# ADMISSION/ICU/ICU/LAST_CAREUNIT					^
		lcu=fcu
		iculcu=ETREE.SubElement(nicu2,'LAST_CAREUNIT')
		iculcu.text=lcu
		
		# ADMISSION/ICU/ICU/INTIME		*
		intime=admit_dt
		icuintime=ETREE.SubElement(nicu2,'INTIME')
		icuintime.text=str(intime)
		
		# ADMISSION/ICU/ICU/OUTTIME										*
		outtime=disch_dt
		icuouttime=ETREE.SubElement(nicu2,'OUTTIME')
		icuouttime.text=str(outtime)
		
		# ADMISSION/ICU/ICU/LOS								- Derived from intime/outtime
		los=admdays_int*24*60
		iculos=ETREE.SubElement(nicu2,'LOS')
		iculos.text=str(los)
		
		# ADMISSION/ICU/ICU/LOS_GRP							^
		los_grp = ''
		if (los<0.5):
			los_grp=' <00100'
		elif ((los>=100) and (los<500)):
			los_grp='00100-<00500'
		elif ((los>=500) and (los<1000)):
			los_grp='00500-<01000'
		elif ((los>=1000) and (los<1500)):
			los_grp='01000-<01500'
		elif ((los>=1500) and (los<2000)):
			los_grp='01500-<02000'
		elif ((los>=2000) and (los<3000)):
			los_grp='02000-<03000'
		elif ((los>=3000) and (los<4000)):
			los_grp='03000-<04000'
		elif ((los>=4000) and (los<5000)):
			los_grp='04000-<05000'
		elif ((los>=5000) and (los<7500)):
			los_grp='05000-<07500'
		elif ((los>=7500) and (los<10000)):
			los_grp='07500-<10000'
		elif ((los>=10000) and (los<15000)):
			los_grp='10000-<15000'
		elif ((los>=20000) and (los<50000)):
			los_grp='20000-<50000'
		elif (los>=50000):
			los_grp='50000+'
		iculos_grp=ETREE.SubElement(nicu2,'LOS_GRP')
		iculos_grp.text=los_grp
		
		# Not adding these
		# ADMISSION/ICU/ICU/CENSUS_EVENT					^
		# ADMISSION/ICU/ICU/CENSUS_EVENT/CENSUS_ID						^
		# ADMISSION/ICU/ICU/CENSUS_EVENT/INTIME							^
		# ADMISSION/ICU/ICU/CENSUS_EVENT/OUTTIME							^
		# ADMISSION/ICU/ICU/CENSUS_EVENT/CAREUNIT								
		# ADMISSION/ICU/ICU/CENSUS_EVENT/DESTINATION								
		# ADMISSION/ICU/ICU/CENSUS_EVENT/DISCHARGE_STATUS								
		# ADMISSION/ICU/ICU/CENSUS_EVENT/COUNT				- Sequence #

		# ADMISSION/ICU/MEDEVENTS							
		# ADMISSION/ICU/MEDEVENTS/EVENT								
		# ADMISSION/ICU/MEDEVENTS/EVENT/NAME			
		# ADMISSION/ICU/MEDEVENTS/EVENT/OCCURANCES
		nmed=ETREE.SubElement(nicu2,'MEDEVENTS')
		
		medlist=[]
		medcnt=sample(hist[sex][agegrp]['MEDCNT'],hist_cnt[sex][agegrp]['MEDCNT'])
		print 'medcnt',medcnt
		for q in range(int(medcnt)):
			med=sample(hist[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/NAME'],hist_cnt[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/NAME'])
			occur_per_day=sample(hist[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES'][med],hist_cnt[sex][agegrp]['ICU/ICU/MEDEVENTS/EVENT/OCCURRENCES'][med])
			occur=int(round(float(occur_per_day)*admdays.days+0.5))
			if (med) not in medlist:
				medlist.append([med,occur])
				nevent=ETREE.SubElement(nmed,'EVENT')
				nname=ETREE.SubElement(nevent,'NAME')
				nname.text=str(med)
				noccur=ETREE.SubElement(nevent,'OCCURRENCES')
				noccur.text=str(occur)
					
		print 'meds', medlist
			
			
		# ADMISSION/POEEVENTS
		# ADMISSION/POEEVENTS/EVENT
		# ADMISSION/POEEVENTS/EVENT/NAME
		# ADMISSION/POEEVENTS/EVENT/OCCURANCES
		npoes=ETREE.SubElement(admission,'POEEVENTS')
		poelist=[]
		poecnt=sample(hist[sex][agegrp]['POECNT'],hist_cnt[sex][agegrp]['POECNT'])
		print 'poecnt',poecnt
		for q in range(int(poecnt)):
			poe=sample(hist[sex][agegrp]['POEEVENTS/EVENT/NAME'],hist_cnt[sex][agegrp]['POEEVENTS/EVENT/NAME'])
			occur_per_day=sample(hist[sex][agegrp]['POEEVENTS/EVENT/OCCURRENCES'][poe],hist_cnt[sex][agegrp]['POEEVENTS/EVENT/OCCURRENCES'][poe])
			occur=int(round(float(occur_per_day)*admdays.days+0.5,0))
			if (poe) not in poelist:
				poelist.append([poe,occur])
				npoe=ETREE.SubElement(npoes,'EVENT')
				nname=ETREE.SubElement(npoe,'NAME')
				nname.text=str(poe)
				noccur=ETREE.SubElement(npoe,'OCCURRENCES')
				noccur.text=str(occur)
				
		print 'poe', poelist
			

		# ADMISSION/LABEVENTS
		# ADMISSION/LABEVENTS/EVENT
		# ADMISSION/LABEVENTS/EVENT/NAME
		# ADMISSION/LABEVENTS/EVENT/OCCURANCES
		nlabs=ETREE.SubElement(admission,'POEEVENTS')
		lablist=[]
		labcnt=sample(hist[sex][agegrp]['LABCNT'],hist_cnt[sex][agegrp]['LABCNT'])
		print 'labcnt',labcnt
		for q in range(int(labcnt)):
			lab=sample(hist[sex][agegrp]['LABEVENTS/EVENT/NAME'],hist_cnt[sex][agegrp]['LABEVENTS/EVENT/NAME'])
			occur_per_day=sample(hist[sex][agegrp]['LABEVENTS/EVENT/OCCURRENCES'][lab],hist_cnt[sex][agegrp]['LABEVENTS/EVENT/OCCURRENCES'][lab])
			occur=int(round(float(occur_per_day)*admdays.days+0.5,0))
			if (lab) not in lablist:
				lablist.append([lab,occur])
				nlab=ETREE.SubElement(nlabs,'EVENT')
				nname=ETREE.SubElement(nlab,'NAME')
				nname.text=str(lab)
				noccur=ETREE.SubElement(nlab,'OCCURRENCES')
				noccur.text=str(occur)
		print 'lab', lablist
	
		# Create a note for every day in the visit
		print 'admdays_int',admdays_int
		for t in range(admdays_int):
			# ADMISSION/NOTEEVENT									*
			# ADMISSION/NOTEEVENT/CHART_TIME									*
			nnoteevent=ETREE.SubElement(admission,'NOTEEVENT')
			
			# ADMISSION/NOTEEVENT/CAREUNIT
			careunit=sample(hist[sex][agegrp]['NOTEEVENT/CAREUNIT'],hist_cnt[sex][agegrp]['NOTEEVENT/CAREUNIT'])
			print 'careunit',careunit
			ncareunit=ETREE.SubElement(nnoteevent,'CAREUNIT')
			ncareunit.text=str(careunit)
			
			# ADMISSION/NOTEEVENT/CAREGIVER
			caregiver=sample(hist[sex][agegrp]['NOTEEVENT/CAREGIVER'],hist_cnt[sex][agegrp]['NOTEEVENT/CAREGIVER'])
			if (not caregiver=='None'):
				caregiver=caregiver+str(int(random.random()*num_caregivers))
			print 'caregiver',caregiver
			ncaregiver=ETREE.SubElement(nnoteevent,'CAREGIVER')
			ncaregiver.text=str(caregiver)
			
			# ADMISSION/NOTEEVENT/TITLE
			title=sample(hist[sex][agegrp]['NOTEEVENT/TITLE'],hist_cnt[sex][agegrp]['NOTEEVENT/TITLE'])
			print 'title',title
			ntitle=ETREE.SubElement(nnoteevent,'TITLE')
			ntitle.text=str(title)
			
			# ADMISSION/NOTEEVENT/CATEGORY
			category=sample(hist[sex][agegrp]['NOTEEVENT/CATEGORY'],hist_cnt[sex][agegrp]['NOTEEVENT/CATEGORY'])
			print 'category',category
			ncategory=ETREE.SubElement(nnoteevent,'CATEGORY')
			ncategory.text=str(category)
			
			# print text_map
			# ADMISSION/NOTEEVENT/TEXT
			num_lines=int(random.random()*avg_note_lines*2)
			textstr=''
			for l in range(num_lines):
					# t1="no text for picd9:", picd9
					# if (picd9 in text_map):
					# t1=random.choice(text_map[picd9].keys())					
				t1=random.choice(text_list)
				textstr+=t1+' '
				# print 'notetext:',t1
			ntext=ETREE.SubElement(nnoteevent,'TEXT')
			ntext.text=str(textstr)
	
		xstr=ETREE.tostring(doc, pretty_print = True)
		# print xstr
		fa.write(xstr)
			# Add to distribution table
			# ^ ADMISSION/HADM_ID
			# ^ ADMISSION/PATIENT/SUBJECT_ID					- Sequential
			# ^ ADMISSION/PATIENT/ADMISSION_DATE					*- Randomized
			# ^ ADMISSION/PATIENT/ADMISSION_DATE_YRMON			**	- Derived 
			# ^ ADMISSION/PATIENT/ADMISSION_DATE_MON_GRP			*	- Derived
			# ^ ADMISSION/PATIENT/ADMISSION_DATE_DAYMON_GRP
			# ^ ADMISSION/PATIENT/ADMISSION_DATE_DAYWEEK_GRP	*	- Derived
			# ^ ADMISSION/PATIENT/ADMISSION_DATE_WEEK_GRP		
			# ^ ADMISSION/PATIENT/DISCHARGE_DATE				*- Derived - Based on selected LOA
			# ^ ADMISSION/PATIENT/DOB							*- Derived - Based Age_at_Admission			
			# ^ ADMISSION/PATIENT/DOD								*- Derived - Based on Age_at_Admission
			# ^ ADMISSION/PATIENT/AGE_AT_DEATH
			# ^ ADMISSION/PATIENT/AGE_AT_DEATH_GRP
			# ^ ADMISSION/PATIENT/SEX
			# ^ ADMISSION/PATIENT/AGE_AT_ADMISSION
			# ^ ADMISSION/PATIENT/AGE_AT_ADMISSION_GRP			  - Derived - Based on Age at Admission
			#^ ADMISSION/PATIENT/HOSPITAL_EXPIRE   				Y only if DOD is not NONE
			#^ ADMISSION/PATIENT/ETHNICITY
			#^ ADMISSION/PATIENT/ADMISSION_TYPE
			#^ ADMISSION/PATIENT/RELIGION
			#^ ADMISSION/PATIENT/ADMISSION_SOURCE
			#^ ADMISSION/PATIENT/MARITAL_STATUS
			#^ ADMISSION/PATIENT/OVERALL_PAYOR_GROUP
			#^ ADMISSION/ICD9								--> Drugs, Procedures
			# ^ ADMISSION/DRG									--> Dependent on primary ICD9
			# ^ ADMISSION/DRG/CODE							^
			# ^ ADMISSION/DRG/ITEMID							^
			# ^ ADMISSION/DRG/DESC							^
			# ^ ADMISSION/DRG/COST_WEIGHT						^
			# ^ ADMISSION/DRG/DISPLAY							^
			# ADMISSION/ICU
			# ADMISSION/ICU/COUNT
			# ADMISSION/ICU/ICU/ICUSTAY_ID
			# ADMISSION/ICU/ICU/FIRST_CAREUNIT					
			# ADMISSION/ICU/ICU/LAST_CAREUNIT					^
			# ADMISSION/ICU/ICU/INTIME										*
			# ADMISSION/ICU/ICU/OUTTIME										*
			# ADMISSION/ICU/ICU/LOS								- Derived from intime/outtime
			# ADMISSION/ICU/ICU/LOS_GRP							^
			# ADMISSION/ICU/ICU/CENSUS_EVENT					^
			# ADMISSION/ICU/ICU/CENSUS_EVENT/CENSUS_ID						^
			# ADMISSION/ICU/ICU/CENSUS_EVENT/INTIME							^
			# ADMISSION/ICU/ICU/CENSUS_EVENT/OUTTIME							^
			# ADMISSION/ICU/ICU/CENSUS_EVENT/CAREUNIT								
			# ADMISSION/ICU/ICU/CENSUS_EVENT/DESTINATION								
			# ADMISSION/ICU/ICU/CENSUS_EVENT/DISCHARGE_STATUS								
			# ADMISSION/ICU/ICU/CENSUS_EVENT/COUNT				- Sequence #
			# ^ ADMISSION/ICU/MEDEVENTS							
			# ^ ADMISSION/ICU/MEDEVENTS/EVENT								
			# ^ ADMISSION/ICU/MEDEVENTS/EVENT/NAME			
			# ^ ADMISSION/ICU/MEDEVENTS/EVENT/OCCURANCES
			# ^ ADMISSION/POEEVENTS
			# ^ ADMISSION/POEEVENTS/EVENT
			# ^ ADMISSION/POEEVENTS/EVENT/NAME
			# ^ ADMISSION/POEEVENTS/EVENT/OCCURANCES
			# ^ ADMISSION/LABEVENTS
			# ^ ADMISSION/LABEVENTS/EVENT
			# ^ ADMISSION/LABEVENTS/EVENT/NAME
			# ^ ADMISSION/LABEVENTS/EVENT/OCCURANCES
			# ^ ADMISSION/NOTEEVENT/CHART_TIME									*
			# ^ ADMISSION/NOTEEVENT/CAREUNIT
			# ^ ADMISSION/NOTEEVENT/CAREGIVER
			# ^ ADMISSION/NOTEEVENT/TITLE
			# ^ ADMISSION/NOTEEVENT/CATEGORY
			# ^ ADMISSION/NOTEEVENT/TEXT


	fa.write("\n</AdmissionList>");
	fa.close()
			
	finish = time.time()
	print 'End UTC: '+str(finish)
	print "Time taken = "+str(finish - stime)+" seconds"
