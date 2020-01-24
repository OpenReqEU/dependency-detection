import requests
import json
import io

URL = "http://localhost:9407/upc/dependency-detection/"
WITHOUT_PERSISTENCE = "json/ontology/"
WITH_PERSISTENCE = "persistence/analysis/"

def initPrompt():
	print("Please type a number to perform a specific action:")
	print("")
	print("WITHOUT PERSISTENCE")
	print("\t 1 - Run a dependency analysis and return results")
	print("")
	print("WITH PERSISTENCE")
	print("\t 2 - Run a dependency analysis")
	print("\t 3 - Get dependencies between requirements")
	print("\t 4 - Remove dependency analysis results")
	print("")
	print("Action number: ", end = '')
	action = int(input()) 
	print("")
	valid = validateAction(action)
	if (not valid):
		print("!!! WRONG ACTION ID")
		print("")
		action = initPrompt()
	return action

def validateAction(action):
	if (action > 0 and action < 5):
		return True
	else:
		return False

def selectKeywordTool():
	print("Select a keyword extraction tool [RULE_BASED|TFIDF_BASED]: ", end = ' ')
	tool = input()
	if (tool != 'RULE_BASED' and tool != 'TFIDF_BASED'):
		print("!!! INVALID KEYWORD TOOL")
		return selectKeywordTool()
	else:
		return tool

def askSyn():
	print("Do you want to apply synonymy? (Y/N): ", end = ' ')
	synonymy = input()
	if (synonymy == 'Y'):
		return selectSynThreshold()
	elif (synonymy == 'N'):
		return -1
	else:
		print("!!! INVALID ANSWER")
		return askSyn()

def selectSynThreshold():
	print("Select a threshold synonymy value [0..1]: ", end = ' ')
	threshold = float(input())
	if (threshold >= 0 and threshold <= 1):
		return threshold
	else:
		print("!!! INVALID SYNONYMY THRESHOLD")
		return selectSynThreshold()

def selectReqFile():
	print("Input the relative path to the file containing the requirements in JSON format (e.g., ./confidential_Apata-Cata_RBC.json): ", end = ' ')
	reqPath = input()
	try:
		with io.open(reqPath, 'r', encoding='utf-8-sig') as json_file:
			#json_object = json.load(json_file)
			#return json_object
			return reqPath
	except EnvironmentError:
		print("!!! WRONG REQUIREMENTS PATH FILE")
		return selectReqFile()

def selectOntologyFile():
	print("Input the relative path to the ontology file (e.g., ./openreq-rail-small.owl): ", end = ' ')
	ontology = input()
	try:
		with io.open(ontology, 'r', encoding='utf-8-sig') as owl_file:
			return ontology
	except EnvironmentError:
		print("!!! WRONG ONTOLOGY PATH FILE")
		return selectOntologyFile()

def selectProject(reqs):
	print("Input the project ID (e.g., Siemens): ", end = ' ')
	project = input()
	with io.open(reqs, 'r', encoding='utf-8-sig') as json_file:
			#json_object = json.load(json_file)
		json_object = json.load(json_file)
		found = False
		for p in json_object['projects']:
			if p['id'] == project:
				found = True
		if (found):
			return project
		else:
			print("!!! PROJECT DOES NOT EXISTS IN REQUIREMENTS DATASET")
			return selectProject(reqs)

def selectAnalysisId():
	print("Input the analysis ID (e.g., 1268948486424): ", end = ' ')
	analysis = input()
	return analysis

def selectReq1():
	print("Input the first requirement ID (e.g., TND_256) or leave empty for full results: ", end = ' ')
	req = input()
	return req

def selectReq2():
	print("Input the second requirement ID (e.g., TND_256) or leave empty for full results: ", end = ' ')
	req = input()
	return req

def sendRequest(path_request, reqs, ontology):
	with io.open(reqs, 'r', encoding='utf-8-sig') as req_file:
		with io.open(ontology, 'r', encoding='utf-8-sig') as ont_file:
			print("")
			print("Sending request...")
			res = requests.post(path_request, files=(("ontology", ont_file), ("json", req_file)))
			print("Response ", res)
			print("")

def runAnalysisAndReturn():
	reqs = selectReqFile()
	ontology = selectOntologyFile()
	project = selectProject(reqs)
	tool = selectKeywordTool()
	syn = askSyn()

	path_request = URL + WITHOUT_PERSISTENCE + "/" + project + "?keywordTool=" + tool
	if (syn == -1):
		path_request += "&synonymy=false"
	else:
		path_request += "&synonymy=true&threshold=" + syn

	sendRequest(path_request, reqs, ontology)

def runAnalysis():
	reqs = selectReqFile()
	ontology = selectOntologyFile()
	project = selectProject(reqs)
	tool = selectKeywordTool()
	syn = askSyn()

	path_request = URL + WITH_PERSISTENCE + "?projectId=" + project + "&keywordTool=" + tool
	if (syn == -1):
		path_request += "&synonymy=false"
	else:
		path_request += "&synonymy=true&threshold=" + syn

	sendRequest(path_request, reqs, ontology)

def getDependenciesBetweenRequirements():
	analysisId = selectAnalysisId()
	req1 = selectReq1()
	req2 = selectReq2()

def deleteDependencyAnalysisResults():
	analysisId = selectAnalysisId()

def main():
	action = initPrompt()

	if (action == 1):
		runAnalysisAndReturn()
	elif (action == 2):
		runAnalysis()
	elif (action == 3):
		getDependenciesBetweenRequirements()
	else:
		deleteDependencyAnalysisResults()

	main()


print("### OPENREQ-DD ###")
print("")
main()