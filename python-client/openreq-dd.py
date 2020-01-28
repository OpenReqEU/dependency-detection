import requests
import json
import io
import time


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
	print("Input the relative path to the file containing the requirements in JSON format (e.g., ./requirements.json): ", end = ' ')
	reqPath = input()
	try:
		with io.open(reqPath, 'r', encoding='utf-8-sig') as json_file:
			#json_object = json.load(json_file)
			#return json_object
			return reqPath
	except EnvironmentError:
		print("!!! WRONG REQUIREMENTS PATH FILE")
		return selectReqFile()
	except FileNotFoundError:
		print("!!! FILE NOT FOUND")
		return selectReqFile()

def selectOntologyFile():
	print("Input the relative path to the ontology file (e.g., ./ontology.owl): ", end = ' ')
	ontology = input()
	try:
		with io.open(ontology, 'r', encoding='utf-8-sig') as owl_file:
			return ontology
	except EnvironmentError:
		print("!!! WRONG ONTOLOGY PATH FILE")
		return selectOntologyFile()
	except FileNotFoundError:
		print("!!! FILE NOT FOUND")
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
	print("Input the analysis ID (e.g., 1580140071546): ", end = ' ')
	analysis = input()
	return analysis

def selectReq1():
	print("Input the first requirement ID (e.g., 236182) or leave empty for full results: ", end = ' ')
	req = input()
	return req

def selectReq2():
	print("Input the second requirement ID (e.g., 236168) or leave empty for full results: ", end = ' ')
	req = input()
	return req

def sendRequest(path_request, reqs, ontology):
	with io.open(reqs, 'r', encoding='utf-8-sig') as req_file:
		with io.open(ontology, 'r', encoding='utf-8-sig') as ont_file:
			print("")
			print("Sending request...")
			res = requests.post(path_request, files=(("ontology", ont_file), ("json", req_file)))
			if (res.status_code == 200):
				print("Response ", res)
				print("")
				return res.json()
			else:
				return -1

def params():
	print("Input params configuration file path (e.g., ./params.json) or leave empty to input custom parameters:", end = ' ')
	params = input()
	print("")

	if (params == ''):
		reqs = selectReqFile()
		ontology = selectOntologyFile()
		project = selectProject(reqs)
		tool = selectKeywordTool()
		syn = askSyn()
	else:
		with io.open(params, 'r', encoding='utf-8-sig') as params_file:
			params_json = json.load(params_file)
			reqs = params_json['requirementsFile']
			ontology = params_json['ontologyFile']
			project = params_json['project']
			tool = params_json['keywordTool']
			print("Dependency analysis configuration")
			print("--------------------------------------")
			print("Requirements file path:", reqs)
			print("Ontology file path:", ontology)
			print("Project ID:", project)
			print("Keyword tool:", tool)
			print("Synonymy:", params_json['synonymy'])
			if (params_json['synonymy'] == 'Y'):
				syn = params_json['threshold']
				print("Threshold:", syn)
			else:
				syn = -1
			print("--------------------------------------")

	return reqs, ontology, project, tool, syn

def runAnalysisAndReturn():
	reqs, ontology, project, tool, syn = params()
	path_request = URL + WITHOUT_PERSISTENCE + "/" + project + "?keywordTool=" + tool
	if (syn == -1):
		path_request += "&synonymy=false"
	else:
		path_request += "&synonymy=true&threshold=" + str(syn)
	millis = int(round(time.time() * 1000))
	response = sendRequest(path_request, reqs, ontology)
	if (response == -1):
		print("!!! There was an error with the request. Please try again or contact an administrator")
		print("")
	else:
		path = './dep-analysis-' + str(millis) + ".json"
		with open(path, 'w') as f:
			json.dump(response, f)
			print("Finished dependency analysis")
			print("--------------------------------------")
			print("Nº dependencies:", len(response['dependencies']))
			print("Dependency analysis results stored at:", path)
			print("--------------------------------------")
			print("")


def runAnalysis():
	reqs, ontology, project, tool, syn = params()
	path_request = URL + WITH_PERSISTENCE + "?projectId=" + project + "&keywordTool=" + tool
	if (syn == -1):
		path_request += "&synonymy=false"
	else:
		path_request += "&synonymy=true&threshold=" + str(syn)
	response = sendRequest(path_request, reqs, ontology)
	if (response == -1):
		print("!!! There was an error with the request. Please try again or contact an administrator")
		print("")
	else:
		print("Finished dependency analysis")
		print("--------------------------------------")
		print("Analysis ID:", response['analysisId'])
		print("--------------------------------------")
		print("")

def sendGetRequest(path_request):
	print("")
	print("Sending request...")
	res = requests.get(path_request)
	if (res.status_code == 200):
		print("Response ", res)
		print("")
		return res.json()
	else:
		return -1

def getDependenciesBetweenRequirements():
	analysisId = selectAnalysisId()
	req1 = selectReq1()
	req2 = selectReq2()
	path_request = URL + WITH_PERSISTENCE + analysisId
	if (len(req1) > 0 and len(req2) > 0):
		path_request += "?req1=" + req1 + "&req2=" + req2
	response = sendGetRequest(path_request)
	if (response == -1):
		print("!!! There was an error with the request. Please try again or contact an administrator")
		print("")
	else:
		print("Dependencies results (nº of dependencies: " + str(len(response['dependencies'])) + ")")
		print("--------------------------------------")
		for dep in response['dependencies']:
			print("Requirement " + dep['fromid'] + " " + dep['dependency_type'] + " " + dep['toid'])
		print("--------------------------------------")
		print("")

def sendDeleteRequest(path_request):
	print("")
	print("Sending request...")
	res = requests.delete(path_request)
	if (res.status_code == 200):
		print("Response ", res)
		print("")
		return 1
	else:
		return -1

def deleteDependencyAnalysisResults():
	analysisId = selectAnalysisId()
	path_request = URL + WITH_PERSISTENCE + analysisId
	response = sendDeleteRequest(path_request)
	if (response == -1):
		print("!!! There was an error with the request. Please try again or contact an administrator")
		print("")
	else:
		print("Dependency analysis " + analysisId + " deleted")
		print("")

def main():
	try:
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
	except ValueError:
		print("\nInvalid value. Please try again\n")
		main()


print("### OPENREQ-DD ###")
print("")
main()