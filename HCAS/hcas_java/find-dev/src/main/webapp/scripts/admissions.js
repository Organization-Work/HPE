$(function(){
	
	var tempID =  localStorage.getItem("storageId");
	
    var $container = $('#record_container');
    var idx = 0;
    var nHighlights = 0;
    
    var HL_CLASSNAME = 'docquery-highlight';
    var CONCEPT_METATAG = "SM_ISA";
    var CONCEPT_ID_ATTR = "CID";
    var CONCEPT_CHILDID_ATTR = "SID";
    var FILTER_HL_TYPE = 'filter';
    var LINK_HL_TYPE = 'hllink';
    var QUERY_HL_TYPE = 'query';
    
    var HL_EMBEDDED_SELECTOR = ".hl-embedded-text";
    var HL_EMBEDDED_SRC_SELECTOR = ".hl-embedded-src";
    
    var selectedNodes = "";
    var isMultiple = false;
    var getPreSelectedNodes = function() {
    	var element = document.getElementById('tree-data');
		// Note: in modern browsers, you could use `element.dataset.data` instead
		// of `getAttribute('data-data')`.
		if (element==null) return [];
		
		var string = element.getAttribute('selectedNodes');
		if (string=="") return [];

		var data = string;
		
		if (data==null) return [];
		
		// Remove the attribute now that we have a copy of the data.
		element.removeAttribute('selectedNodes');
		return data;
    }
   
	
	var getConceptData = function() {
		// Read the JSON-formatted data from the DOM.
		var element = document.getElementById('tree-data');
		// Note: in modern browsers, you could use `element.dataset.data` instead
		// of `getAttribute('data-data')`.
		if (element==null) return "[]";
		
		var string = element.getAttribute('conceptData');
		if (string=="") return "[]";

		var data = JSON.parse(string);
		
		if (data==null) return "[]";
		
		// Remove the attribute now that we have a copy of the data.
		element.removeAttribute('conceptData');
		return data;
	};
	var getContextData = function() {
		// Read the JSON-formatted data from the DOM.
		var element = document.getElementById('tree-data');
		// Note: in modern browsers, you could use `element.dataset.data` instead
		// of `getAttribute('data-data')`.
		if (element==null) return "[]";

		var string = element.getAttribute('contextData');
		if (string=="") return "[]";

		var data = JSON.parse(string);
		if (data==null) return "[]";

		// Remove the attribute now that we have a copy of the data.
		element.removeAttribute('contextData');
		return data;
	};
	var getRelationData = function() {
		// Read the JSON-formatted data from the DOM.
		var element = document.getElementById('tree-data');
		if (element==null) return "[]";
		// Note: in modern browsers, you could use `element.dataset.data` instead
		// of `getAttribute('data-data')`.
		var string = element.getAttribute('relData');
		if (string=="") return "[]";
		var data = JSON.parse(string);
		if (data==null) return "[]";
		// Remove the attribute now that we have a copy of the data.
		element.removeAttribute('relData');
		return data;
	};
	var getNegationData = function() {
		// Read the JSON-formatted data from the DOM.
		var element = document.getElementById('tree-data');
		if (element==null) return "[]";
		// Note: in modern browsers, you could use `element.dataset.data` instead
		// of `getAttribute('data-data')`.
		var string = element.getAttribute('negData');
		if (string=="") return "[]";
		var data = JSON.parse(string);
		if (data==null) return "[]";
		// Remove the attribute now that we have a copy of the data.
		element.removeAttribute('negData');
		return data;
	};
	var groupBy1='hier'
	var groupBy2=''
	var	groupByConcept='name'
	var sortKey='begin'
		

		
	function groupConceptTree(concepts,conceptData,parentConceptListMap,conceptTreeMap,negMap)
	{
	
	if (conceptData=="[]") return null;
		// GroupBy 2 Levels
		var groups;
		if (groupBy1!='' && groupBy2!='') {
			var groups = _.groupBy( conceptData, groupBy1 );
			
			// groupByConcept
			for ( var key in groups ) {
				group2=_.groupBy(groups[key], groupBy2);				
				for (var key2 in group2) {
					group2[key2]=_.sortBy(group2[key2], sortKey);
					group3=_.groupBy(group2[key2],groupByConcept);
					group2[key2]=group3;
				
				}
				groups[key]=group2;
			}
			
			// Update the Concept Data
			for ( var key in groups) {
				var node= {"id":"CONCEPT_"+key.toUpperCase(),"text":key.toUpperCase(),"state":{"disabled":false,"selected":false},"children":[]};
				for (var key2 in groups[key]) {
					var node2={"id":"CONCEPT_"+key.toUpperCase()+"_"+key2,"text":key2,"state":{"opened":false,"disabled":false,"selected":false},"children":[]};
					for (var key3 in groups[key][key2]) {
						var ctid="CONCEPT_"+key.toUpperCase()+"_"+key2+"_"+key3
						var node3={"id":ctid,"text":key3,"state":{"disabled":false,"selected":false},"children":[]};
						var conceptFullName=key3.toUpperCase()+" ("+key2.toUpperCase()+")"
						conceptTreeMap[conceptFullName]=ctid;
						if (!(ctid in hidMap)) {
							hidMap[ctid]=[];
						}
						var pFound=false;
						for (var key4 in groups[key][key2][key3]) {
							// tid=node3.id+"_"+groups[key][key2][key3][key4].hid;
							// hidMap[tid]=groups[key][key2][key3][key4].hid;
							// var node4={"id":tid,"data":groups[key][key2][key3][key4],"text":groups[key][key2][key3][key4].text,"icon":false,"state":{"hidden":true,"disabled":true,"selected":false},"children":[]};
							// node3.children.push(node4);
							hid=groups[key][key2][key3][key4].hid;
							if (!(hid in negMap)) {
								hidMap[ctid].push(hid);
								pFound=true;
							}
						}
						if (pFound) {
							node2.children.push(node3);
						}   
					}
					node.children.push(node2);
				}
				concepts.children.push(node);
			}		
		}
		// GroupBy 1 element
		else if (groupBy1!='' && groupBy2=='') {
			var groups = _.groupBy( conceptData, groupBy1 );
			
			// groupByConcept
			for ( var key in groups ) {
				groups[key]=_.sortBy(groups[key], sortKey);
				group2=_.groupBy(groups[key],groupByConcept);
				groups[key]=group2;
				// groups[key]=_.sortBy(group2, sortKey);
			}
			
			// Update the Concept Data
			for ( var key in groups) {
				var node= {"id":"CONCEPT_"+key.toUpperCase(),"text":key.toUpperCase(),"state":{"disabled":false,"selected":false},"children":[]};
				for (var key2 in groups[key]) {
					var ctid="CONCEPT_"+key.toUpperCase()+key2
					var node2={"id":ctid,"text":key2,"state":{"opened":false,"disabled":false,"selected":false},"children":[]};
					var conceptFullName=key2.toUpperCase()+" ("+key.toUpperCase()+")"
					conceptTreeMap[conceptFullName]=ctid;
					if (!(ctid in hidMap)) {
						hidMap[ctid]=[];
					}
					var pFound=false;
					for (var key3 in groups[key][key2]) {
						// tid=node2.id+"_"+groups[key][key2][key3].hid;
						// hidMap[tid]=groups[key][key2][key3].hid;
						// var node3={"id":tid,"text":groups[key][key2][key3].text,"icon":false,"state":{"hidden":true,"disabled":true,"selected":false},"children":[]};
						// TODO, implement for other grouping structures
						
						// Get all the parents for the node (which includes this node)
						hid=groups[key][key2][key3].hid;
						if (!(hid in negMap)) {
							hidMap[ctid].push(hid);
							pFound=true;
						}
						
						
					/*	if (hid in conceptParentTreeMap) {
							parentlist=conceptParentTreeMap[hid];
								if (hid in conceptTreeMap
								conceptTreeMap[groups[key][key2][key3].text]=tid
							}
						}
					*/
						// node3.data=groups[key][key2][key3]
						// node2.children.push(node3);
					}
					if (pFound) {
						node.children.push(node2);
					}
				}

				concepts.children.push(node);
			}		
		} else if (groupBy1=='' && groupBy2=='') {
		
			// groupByConcept
			var groups=_.sortBy(groups, sortKey);
			var groups=_.groupBy(conceptData,groupByConcept);
			for (var key2 in groups) {
				var ctid="CONCEPT_"+key2;
				var node2={"id":ctid,"text":key2,"state":{"opened":false,"disabled":false,"selected":false},"children":[]};
				var conceptFullName=key2.toUpperCase();
				conceptTreeMap[conceptFullName]=ctid;
				if (!(ctid in hidMap)) {
					hidMap[ctid]=[];
				}
				var pFound=false;
				for (var key3 in groups[key2]) {
//					tid=node2.id+"_"+groups[key2][key3].hid;
//					hidMap[tid]=groups[key2][key3].hid;
//					var node3={"id":tid,"text":groups[key2][key3].text,"icon":false,"state":{"hidden":true,"disabled":true,"selected":false},"children":[]};
//					node3.data=groups[key2][key3]
//					node2.children.push(node3);
					hid=groups[key2][key3].hid;
					if (!(hid in negMap)) {
						hidMap[ctid].push(hid);
						pFound=true;
					}
				}
				if (pFound) {
					concepts.children.push(node2);
				}
			}
			
		}	
		
			
		return groups;
	}
	
	function groupNegTree(negTerms,negData,negMap,conceptData)
	{
		if (negData=="[]") return null;

		var cnode={"id":"NEG_CONCEPTS","text":"Concepts","state":{"opened":false,"disabled":false,"selected":false},"children":[]};
		negTerms.children.push(cnode);

		var tnode={"id":"NEG_TERMS","text":"Terms","state":{"opened":false,"disabled":false,"selected":false},"children":[]};
		negTerms.children.push(tnode);

		groups=_.sortBy(negData, "begin");
		group2=_.groupBy(groups,"text");
		groups=group2;
		for (var key in groups) {
			tid="NEG_"+key.toUpperCase();
			var node= {"id":tid,"text":key,"state":{"disabled":false,"selected":false},"children":[]};
			if (!(tid in hidMap)) {
				hidMap[tid]=[];
			}
			for (var key2 in groups[key])  {
				var node2= {"id":tid+"_"+groups[key][key2].hid,"text":key,"state":{"disabled":false,"selected":false},"children":[]};
				hid=groups[key][key2].hid;
				// node.children.push(node2);
				hidMap[tid].push(hid);
				// hidMap[tid]=[groups[key][key2].hid];
			}
			tnode.children.push(node);
		}
		
		// Add all negated concepts to negated concept node
		
		var groups=_.sortBy(conceptData, "begin");
		var group2=groups
		var groups=_.groupBy(group2,"text");
		for (var key2 in groups) {
			var ctid="NEG_CONCEPT_"+key2;
			var node2={"id":ctid,"text":key2,"state":{"opened":false,"disabled":false,"selected":false},"children":[]};
			var conceptFullName=key2.toUpperCase();
			conceptTreeMap[conceptFullName]=ctid;
			if (!(ctid in hidMap)) {
				hidMap[ctid]=[];
			}
			var pFound=false;
			for (var key3 in groups[key2]) {
				hid=groups[key2][key3].hid;
				if ((hid in negMap)) {
					hidMap[ctid].push(hid);
					pFound=true;
				}
			}
			if (pFound) {
				cnode.children.push(node2);
			}
		}
			
		// groupByConcept




		return groups;
	}


	var genFolderTree=function() {
		folderData= { "types" : {
		    "default" : {"icon" : "/"},"demo" : {"icon" : "/" }}, 
		    "plugins" : [ "wholerow", "checkbox"],"core":{"checkbox" : {"keep_selected_style" : false},"check_callback": true, "data":[]},expand_selected_onload : false};
		folderData.core.data[0]={"id":"FOLDERS","text": "Folders","state":{"opened":true,"disabled":false,"selected":false},"children":[]};
		var foldersNode=folderData.core.data[0];
		groupFolders(foldersNode);
		return folderData;

	}
	
	function buildParenttoConceptListMap(relMap,relData,conceptData,isRoot)  {
		
		for (nodeKey in relData){
			node=relData[nodeKey];
			nodeid=node.hid;
			nodename=node.text;
			
			srcStr=node.sources;
			if (srcStr) {
				srcSrcs=srcStr.split(" ");
				for (i in srcSrcs) {
					srcid=srcSrcs[i];
					list=[];
					if (relMap[nodename]==null) {
						relMap[nodename]=[];
					}
					relMap[nodename].push(srcid);
				}
			}						
		}
		// Add the concepts 
		for (nodeKey in conceptData){
			node=conceptData[nodeKey];
			nodeid=node.hid;
			nodename=node.name;
			
			if (relMap[nodename]==null) {
				relMap[nodename]=[];
			}
			relMap[nodename].push(nodeid);
		}		
	}

	var parentConceptListMap={};
	var negMap={};
	
    var buildNegationMap=function(contextData) {
		negMap={};
		for (nodeKey in contextData){
			node=contextData[nodeKey];
			if (node.cType=="Negation") {
				negMap[node.appliesTo]=node.hid;
			}
		}
	}

	
	var genTree=function(conceptData,relData,contextData,negData) {
		// Generate Tree JSON with settings
		// { "plugins" : [ "wholerow", "checkbox" ], "core" : { "data" : .... }}
		/* 
{
  id          : "string" // will be autogenerated if omitted
  text        : "string" // node text
  icon        : "string" // string for custom
  state       : {
    opened    : boolean  // is the node open
    disabled  : boolean  // is the node disabled
    selected  : boolean  // is the node selected
  },
  children    : []  // array of strings or objects
  li_attr     : {}  // attributes for the generated LI node
  a_attr      : {}  // attributes for the generated A node
}
		*/
		
		
		
		treeData= { "types" : {
		    "default" : {"icon" : "/"},"demo" : {"icon" : "/" }},
		    "checkbox" : {"keep_selected_style" : false},    
		    "plugins" : [ "wholerow", "checkbox","search"],"core":{"check_callback": true, "multiselect":false,"data":[]},"checkbox":{"three_state":true,"keep_selected_style" : false},expand_selected_onload : false};
		
	
		var hideFlag=true;
		if (typeof conceptData !== 'undefined' && conceptData.length > 0) {
			hideFlag=false;
		}
		
		 // Add Top Level Nodes
		treeData.core.data[0]={"id":"LABELS","text": "Labels","state":{"opened":true,"disabled":false,"selected":false,"hidden":true},"children":[]};		
		treeData.core.data[1]={"id":"FEATURES","text": "Features","state":{"opened":false,"disabled":false,"selected":false,"hidden":hideFlag},"children":[]};
		var features=treeData.core.data[1];
		
		features.children[0]={"id":"CONCEPT","text":"Concepts","state":{"disabled":false,"selected":false},"children":[],"edata":{"param1":"test2"}};
		features.children[1]={"id":"RELATION","text":"Related Concepts","state":{"disabled":false,"selected":false,"hidden":true},"children":[]};
		features.children[2]={"id":"NEG","text":"Negation","state":{"disabled":false,"selected":false},"children":[]};
		features.children[3]={"id":"CONTEXT","text":"Contexts","state":{"disabled":false,"selected":false,"hidden":true},"children":[]};
		

		var concepts=features.children[0];
		
		var negTerms=features.children[2];	
		var contextTerms=features.children[3];
		var relTerms=features.children[1];

		
		var featureData=negData.concat(conceptData).concat(relData);

		// Builds map of negated terms and their hints
		buildNegationMap(contextData);

		// builds a map for each feature all of its sources to the root.
		buildParenttoConceptListMap(parentConceptListMap,relData,conceptData,true);
		
		groupConceptTree(concepts,conceptData,parentConceptListMap,conceptTreeMap,negMap);
		groupNegTree(negTerms,negData,negMap,conceptData);
		
		// groupContextTree(contextTerms,contextData,featureData);
		// groupRelTree(relTerms,relData,featureData);
		
		
		// console.log( relData );
		
		
		/*
		var result = groupBy(conceptData, function(item)
				{
				  return [item.name, item.source];
				});

		// console.log(JSON.stringify(conceptData));
		console.log(JSON.stringify(result));
		*/
		
		// Add Concepts to Tree
		
		
		
		
		return treeData;
	};
	
	// select all nodes which were marked as preselected in previous / next doc
	function selectPreSelectedNodes() {
		var nodestring = getPreSelectedNodes();
		var nodes = [];
		if(nodestring  && typeof nodestring !== 'undefined'&& nodestring.length > 0) {
			nodes = nodestring.split(',');
		}		
		if(nodes && typeof nodes !== 'undefined' && nodes.length > 0) {
			for(i in nodes) {
				var node = nodes[i];
				$("#navtree").jstree('select_node', '#'+node);				
			}		
		}		
	}
	

	// Get the data, then process it.
	var conceptData = getConceptData();	       
	var relData = getRelationData();	       
	var contextData = getContextData();	       
	var negData = getNegationData();	       
	
	var hidMap={};
	var conceptTreeMap={};
	var conceptplusMap={};
	var treedata=genTree(conceptData,relData,contextData,negData);
	var folderdata=genFolderTree();
	$('#navtree').jstree(treedata).bind("loaded.jstree", function() {
		setTimeout(function() {
		onDocsLoaded();
		// selectPreSelectedNodes();
		},10)
	}); 
	
	$('#navtree').jstree(treedata);
	$('#foldertree').jstree(folderdata); 
	
	// Render iframes
	var ifrms=document.getElementsByClassName("docview_iframe_content")
	var i;
	var styleSheets='<link rel="stylesheet" type="text/css" href="https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/2.3.2/css/bootstrap.min.css">' + 
		'<link rel="stylesheet" type="text/cs href="../css/typeaheadjs-${config.version}.css">' +
		' <link rel="stylesheet" type="text/css" href="../css/bootstrap-datetimepicker.min-${config.version}.css"> ' + 
		' <link rel="stylesheet" type="text/css" href="../css/admissions-${config.version}.css">' +
		' <link rel="stylesheet" type="text/css" href="../css/search.filters-${config.version}.css">' +  
		' <link rel="stylesheet" type="text/css" href="../resources/css/cupertino/jquery-ui-1.8.23.custom.css">'
	for (i=0;i<ifrms.length;i++) {
		doc=ifrms[i].contentDocument || ifrms[i].contentWindow.dcoument;
		str=ifrms[i].getAttribute("content");
			doc.open();
		doc.write(str);
		doc.close();
		var headNodes=doc.querySelectorAll("head");
		var headNode;
		if (headNodes.length>0) {
			headNode=headNodes[0]
		} else {
			headNode=doc.createElement("head");
			doc.documentElement.insertBefore(headNode,doc.documentElement.childNodes[0]);
		}
		headNode.innerHTML=styleSheets;
	}

	// build a map 
	$alltags=$('htag');
	$allframetags=$(".docview_iframe_content").contents().find('htag')
	var hlmap={}
	
	$alltags.each(function( index ) {
 	   atlist=$(this).attr("l");
 	   if (atlist) {
 		   atarr=atlist.split(',');
 		   for (k in atarr) {
 			   if (hlmap[atarr[k]]==null) {
 				   hlmap[atarr[k]]=[];
 			   }
 			   hlmap[atarr[k]].push([0,atlist]);
 		   }
 	   }
    });
	
	$allframetags.each(function( index ) {
 	   atlist=$(this).attr("l");
 	   if (atlist) {
 		   atarr=atlist.split(',');
 		   for (k in atarr) {
 			   if (hlmap[atarr[k]]==null) {
 				   hlmap[atarr[k]]=[];
 			   }
 			   hlmap[atarr[k]].push([1,atlist]);
 		   }
 	   }
    });
	
	// Highlight based on selected nodes
    function updateTreeHighlights(hlist) {              
        
    	    //   clearHighlight();
           idx = 0;
           
           var $concepts = null;
           var conceptSelectors = [];
           
    	   // Select all doc elements with the hid attribute
    	   $alltags=$('htag');
 		   $allframetags=$(".docview_iframe_content").contents().find('htag');
		   $alltags=$alltags.add($allframetags);
		   $alltags.removeClass('docquery-highlight');
    	   $alltags.removeAttr('highlightNum');
    	   $alltags.removeAttr('hChild');
    	   if($('span').attr('data-field')){
       		$(this).removeClass("docquery-highlight");
       		$(".data-section").find("span").removeClass(".docquery-highlight.active");
       		$(".data-section").find("span").removeClass("docquery-highlight.active")
    	   }
    	 
		   for (k in hlist) {
               var cid = hlist[k];
     		   if (hlmap[cid]) {
     			   for (d in hlmap[cid]) {
					    var $selected;
						var selector='htag[l="' + hlmap[cid][d][1] + '"]'
						if (hlmap[cid][d][0]==0) {
							$selected=$(selector)
						} else {
							$selected=$(".docview_iframe_content").contents().find(selector)	
						}
     			   		
						if (d==0) {
     			   			conceptSelectors.push($selected);
         					$selected.addClass('docquery-highlight');
         					$selected.attr('hNum',cid);
						}
						else {
         					$selected.attr('hNum',cid);
							$selected.addClass('docquery-highlight2');
						}
     			   }
			   }
            }
           
		   $concepts = $(conceptSelectors);

            toggleHlNavLink($concepts);
			scrollTo(idx);  
    }
/*	
    $("#foldertree").bind(
            "select_node.jstree", function(evt, data){
                // var folderMeta = $(this).closest('.documentfolder').data('documentfolder');
                var url = 'ajax/documentfolder/tag.json';
                var folderId='';
                var reference = Reference;
               
    		  	for (i in data.selected) {
    		  		folderid=data.selected[i];
	               $.ajax(url, {
	                    type: 'POST',
	                    data: {
	                        id: folderid,
	                        ref: reference
	                    }
	                });
    		  	}  
             }
    );
*/
    $("#foldertree").bind(
            "changed.jstree", function(evt, data){
                //selected node object: data.inst.get_json()[0];
                //selected node text: data.inst.get_json()[0].data
            	if (data && data.node && data.node.id=='FOLDERS') {
            		// All nodes
        		  	for (i in data.children) {
        		  		updateNode=data.children[i];
                      	if (data.action=='select_node') {
                    		var url =	'ajax/documentfolder/tag.json';
                            var reference = Reference;
                            var folderId=updateNode.id;
                            $.ajax(url, {
                                type: 'POST',
                                data: {
                                    id: folderId,
                                    ref: reference
                                }
                            });	
                    	} else if (data.action=='deselect_node') {
                       		var url =	'ajax/documentfolder/untag.json';
                            var reference = Reference;
                            var folderId=updateNode.id;
                            $.ajax(url, {
                                type: 'POST',
                                data: {
                                    id: folderId,
                                    ref: reference
                                }
                            });	

                    	}           		
        		  	}
            	} else {
            		// Single Noe
                   	if (data.action=='select_node') {
                		var url =	'ajax/documentfolder/tag.json';
                        var reference = Reference;
                        var folderId=data.node.id;
                        $.ajax(url, {
                            type: 'POST',
                            data: {
                                id: folderId,
                                ref: reference
                            }
                        });	
                	} else if (data.action=='deselect_node') {
                   		var url =	'ajax/documentfolder/untag.json';
                        var reference = Reference;
                        var folderId=data.node.id;
                        $.ajax(url, {
                            type: 'POST',
                            data: {
                                id: folderId,
                                ref: reference
                            }
                        });	

                	}           		
            	}
            }
    );
	
	$('#navtree').on("changed.jstree", function (e, data) {
		  	// console.log("The selected nodes are:");
			if(data.action && (data.action == 'deselect_node' || data.action == 'select_node')) {
				if(top.SearchEvents && top.SearchEvents.saveSelectedNodes) {
					//top.SearchEvents.saveSelectedNodes(data.selected);
				}			
			}
			if(data.action && (data.action == 'ready')) {
				var nodes = getPreSelectedNodes();
				if(nodes && typeof nodes !== 'undefined' && nodes.length > 0) {
					data.selected = nodes;
				}
			}
		    hlist=[]
		  	for (i in data.selected) {
				// if (hidMap[data.selected[i]]!=null) {
		  		// 	hlist.push(hidMap[data.selected[i]]);
		  		// }
				for (k in hidMap[data.selected[i]]) {
					hlist.push(hidMap[data.selected[i]][k]);
				}
		  		$('#navtree').jstree("select_node", "#"+data.selected[i]); 
		    }
		  	// console.log(hlist);
		  	updateTreeHighlights(hlist);
		 
		  });
	 
	/* Populate Folder Hierarchy */
	/* ------------------------- */
	
	function isDocTagged(folderId) {
		var url =	'ajax/documentfolder/istag.json';
        var reference = Reference;        
        var selected = $.ajax(url, {
            type: 'POST',
            data: {
                id: folderId,
                ref: reference
            },
            success: function(response) {
                // For example, filter the response
                callback(filtered_response);
            }

        });
        return selected;
    }
	function groupFolders(foldersNode) { 
	    if (top.SearchEvents && top.SearchEvents.getCheckedFolders) {
	        var folders = top.SearchEvents.getCheckedFolders();
	
	        var foldersById = _.reduce(folders, function(memo, folder){
	            memo[folder.id] = folder;
	            return memo;
	        }, {});
	
	        var checked = {};
	
	        _.each(folders, function(folder) {
	            checked[folder.id] = folder;
	
	            if (!foldersById[folder.id]) {
	                // this is a folder the document was tagged with, but isn't currently selected.
	                // add it to the list of folders to display.
	                folder.existing = true;
	                folders.push(folder);
	            }
	        });
	
	        if (folders.length) {
	 
	            folders.sort(function(a, b){
	                var al = a.name;
	                var bl = b.name;
	                return al < bl ? -1 : al > bl ? 1 : 0;
	            });
	            
	            
	            
	            for ( var x in folders) {
	            	var selected = false;
	            	var url =	'ajax/documentfolder/istag.json';
	                var reference = Reference; 
	            	var isFolderChecked=(checked[folders[x].id]!=null);
	            	if(isFolderChecked) {
	            		 (function (x) {
	            			 $.ajax(url, {
	 	                        type: 'POST',
	 	                        data: {
	 	                            id: folders[x].id,
	 	                            ref: reference
	 	                        },
	 	                        async: false
	 	                    }).done(function(response) {
	 	                    	selected = response;
	 	                    }) 
	            		 })(x);

	            		
	            		//selected = isDocTagged(folderId);
	            		
	            	}
	    			var node= {"id":folders[x].id,"text":folders[x].name,"li_attr":{"title":folders[x].fullPath},"state":{"disabled":false,"selected":selected},"children":[]};
	    			foldersNode.children.push(node);
	            }	
	        	
	        	
	        	
	        /*
	        	$fieldList.addClass('showing-folder-tags');
	            var $folderTags = $('<div id="folder-tags"></div>').insertAfter($fieldList);
	
	            if ($filtersViewer) {
	                $filtersViewer.addClass('showing-folder-tags');
	                $folderTags.addClass('showing-filters-viewer')
	            }
	
	            folders.sort(function(a, b){
	                var al = a.label;
	                var bl = b.label;
	                return al < bl ? -1 : al > bl ? 1 : 0;
	            });
	
	            var documentFolderTemplate = _.template($.resGet('../templates/search/documentfolder.tagger.template'), undefined, {variable: 'ctx'});
	            $folderTags.html(documentFolderTemplate({
	                checked: checked,
	                folders: folders
	            })).on('change', 'input[type=checkbox]', function(){
	                var folderMeta = $(this).closest('.documentfolder').data('documentfolder');
	                var url = this.checked ? 'ajax/documentfolder/tag.json' : 'ajax/documentfolder/untag.json';
	
	                var reference = Reference;
	
	                $.ajax(url, {
	                    type: 'POST',
	                    data: {
	                        id: folderMeta.id,
	                        ref: reference
	                    }
	                });
	            });
	        */
	        	
	        }
	    }
	}
	  
	  
	$("#q").keyup(function(e) {
		 e.preventDefault();
		$("#navtree").jstree(true).search($("#q").val());
	});	
	$("#s").submit(function(e) {
		 e.preventDefault();
	});	
	
	
	$('#tab-content').focus(function() {
        $(this).toggleClass('no-selection', false);
    });
    
    $('#tab-content').blur(function() {
        $(this).toggleClass('no-selection', true);
    });
    
    $('a.external-content-link').click(function() {
        var url = $(this).attr('href');
        var $contentDiv = $(this).closest('div').find('div.external-content');
        
        if ($contentDiv.hasClass('loaded')) {
            $contentDiv.toggleClass('hide');
        } else {
            $contentDiv.empty();
            //show loading...
            $contentDiv.toggleClass('loading', true);
            $.ajax(url, {
                contentType: 'application/json',
            }).done(function(response){
                $contentDiv.toggleClass('error', false);
                
                if (response.success) {
                    $contentDiv.append(response.result);
                    $contentDiv.addClass('loaded');
                } else {
                    $contentDiv.append(response.error);
                    $contentDiv.toggleClass('error', true);
                }
    
            }).error(function(response){
                    $contentDiv.append("Failed to fetch data from server: " + response.statusText);
                    $contentDiv.toggleClass('error', true);
                
            }).always(function() {
                
                $contentDiv.toggleClass('loading', false);
            });
        }
        
        return false;
    });

    var $fieldList = $('#agentstore-fields').on('click', 'h3', function(){
        // allows toggling visibility of specific nodes in the SNOMED supercategory
        $(this).nextUntil('h3').toggle();
    });
    
    $fieldList.on('click', '.highlight-link', function() {
        var linkName = $(this).text();
        var conceptId = $(this).attr('name');
        var hlfield = {name: linkName, value: conceptId, type: LINK_HL_TYPE};
        
        var prevHlField = top.SearchSettings && top.SearchSettings.getDocviewHlField();
        if (!prevHlField || isConceptPlus(prevHlField.field.name) || prevHlField.field.value.indexOf(linkName.toUpperCase()) === -1) {
            $('.hlclicked').removeClass('hlclicked');
            top.SearchSettings.setDocviewHlField({field: hlfield});
            conceptLinksHighlight($(this))
            
        } else {
            scrollTo(idx);
        }
        
        return false;
    });
    

    var $docContainer = $('#cboxContent');
    var $filtersViewer;
    
    var $navTabs = $('.nav-tabs a[data-toggle="tab"]');


    $container.on('click', '.admission h4', function(){
        // clicking on the h4 header of an admission toggles visibility of related information, so you can shrink it
        $(this).siblings().toggle();
    });
    
    $navTabs.click(function() {
        if (top.SearchSettings.setActiveViewTab) {
            top.SearchSettings.setActiveViewTab($(this).attr('href'));
        }
        
    });
    
    var updateOnScroll = true;
    var $scrollerBody = $('.tab-content');

    var $admissionsScroller = $('#admissions-scroller').on('click', '.admission-link', function(){
        var $admission = $(this);
        var admission = $admission.data('admission');
        var $tab = $('.tab-pane.active');
        var $el = $tab.find('.admission[data-admission='+admission+']');

        updateOnScroll = false;

        if (!($scrollerBody[0].scrollHeight > $scrollerBody[0].clientHeight)) {
            // if there's no scroller (too short to scroll), we immediately trigger update of the button
            setActiveLink();
        }
        else if ($el.length) {
            scrollToEl($el, 0, false, setActiveLink);
        }
        else {
            updateOnScroll = true;
        }

        function setActiveLink() {
            $admission.addClass('scrolled-link').siblings('.admission-link').removeClass('scrolled-link');
            updateOnScroll = true;
        }
    });
    
    var findMatchedConceptLinks = _.memoize(function(parentId) {
        return getChildConceptNodes(parentId, []);
    });
    
    

    if ($admissionsScroller.length) {
        $scrollerBody.scroll(showActiveNav);

        $('a[data-toggle=tab]').on('shown', function(){
            var $admission = $('.scrolled-link');
            var admission = $admission.data('admission');

            if (admission) {
                updateOnScroll = false;
                scrollToEl($('.tab-pane.active').find('.admission[data-admission='+admission+']'), 0, true, function(){
                    $admission.addClass('scrolled-link').siblings('.admission-link').removeClass('scrolled-link');
                    updateOnScroll = true;
                });
            }
        });

        showActiveNav();
    }

    function showActiveNav() {
        if (!updateOnScroll) {
            return;
        }

        var $scrollerDom = $scrollerBody[0];
        var pos = $scrollerDom.scrollTop;
        var $targets = $('.active .admission', $scrollerDom);

        if (!$targets.length) {
            return;
        }

        var $selected = $targets.first();

        for (var ii = 0; ii < $targets.length; ++ii) {
            if ($targets[ii].offsetTop > pos) {
                break;
            }
            $selected = $targets.eq(ii);
        }

        var $link = $admissionsScroller.find('.admission-link[data-admission='+$selected.data('admission')+']');
        $link.addClass('scrolled-link').siblings('.admission-link').removeClass('scrolled-link');
    }

    if (top.SearchSettings && !$('#restricted').length) {
        var PROCESSOR = {
            NUMERIC: function(val) {
                var split = _.map(val.split(','), function(a){
                    return Number(a);
                });

                return split.length === 1 ? split[0] : split;
            },
            DATE: function(val, widget){
                var split = _.map(val.split(','), function(a){
                    var date = widget.filterFieldWidget.datepicker1.parseDate(a);
                    // try to parse the date using the datepicker's format; if it fails, just use generic date parsing
                    return date ? date.getTime() : new Date(a).getTime();
                });

                return split.length === 1 ? split[0] : split;
            },
            PARARANGES: function(val) {
                var split = val.split(',');
                split[0] = $.isNumeric(split[0]) ?  Number(split[0]) : -Infinity;
                if (split.length > 1) {
                    split[1] = $.isNumeric(split[1]) ? Number(split[1]) : Infinity;
                }

                return split;
            }
        };

        var OP = {
            GT: function(a, b){return a > b;},
            GE: function(a, b){return a >= b;},
            LT: function(a, b){return a < b;},
            LE: function(a, b){return a <= b;},
            EQ: function(a, b){return a == b;},
            NE: function(a, b){return a != b;},
            RANGE: function(a, b){
                return b.length === 2 && a >= b[0] && a < b[1];
            },
            BEFORE: function(a, b){return a < b;},
            AFTER: function(a, b){return a > b;},
            BETWEEN: function(a, b){
                return b.length === 2 && a >= b[0] && a <= b[1];
            },            
            IS: function(a, b){return a.toUpperCase() === b.toUpperCase();},
            IS_NOT: function(a, b){return a.toUpperCase() != b.toUpperCase();},
            CONTAINS: function(a, b){return a.toUpperCase().indexOf(b.toUpperCase()) !== -1;},
            NOT_CONTAINS: function(a, b){return a.indexOf(b) === -1;},
            IS_RANGE: function(a, b) {
                return b.length === 2 && (a >= b[0] && a < b[1]);
            },
            IS_NOT_RANGE: function(a, b) {
                return b.length === 2 && (a < b[0] || a >= b[1]);
            }
        };

        var filters = top.SearchSettings.getCurrentSearchFilters();
        var query = $.trim(filters.query);
        var hasQuery = query.replace('*', '');
        var filterGroup = filters.filterGroup;

        if (filterGroup || hasQuery) {
            $fieldList.addClass('showing-filters-viewer');
            
   /*       var pnode=$filterViewer;
            $filterViewer=$filterViewer.insertBefore($('<div id="filters-viewer"></div>'),$fieldList.firstChildElement);
            $filterViewer.append('<h5>Query Filters</h5></form>')
   */
            
            $filtersViewer = $('<div id="filters-viewer"></div>').insertBefore($fieldList).append(
                '<h5>Query Filters</h5></form>'
            );
            
        }

        if (hasQuery) {
            $(_.template('<div><fieldset class="filter-query"><span>Query</span><input type="text" id="queryTextInput" value="<%-query%>" disabled></fieldset>', { query: query }))
            .appendTo($filtersViewer).click(function(){
                $('.hlclicked').removeClass('hlclicked');
                
                var $input = $(this).find('input[type="text"]');
                $input.addClass('hlclicked');


                var hlfield = {name: 'querytext', value: $input.val(), type: QUERY_HL_TYPE};
                if (top.SearchSettings) {
                    top.SearchSettings.setDocviewHlField({field: hlfield});
                }
                
                var $toHighlight = $('.autn-highlight');
                
                idx = 0;
                toggleHlNavLink($toHighlight);
                docqueryHighlight($toHighlight, true);

								$(HL_EMBEDDED_SELECTOR).toggleClass('hidden', true);
								$(HL_EMBEDDED_SRC_SELECTOR).toggleClass('hidden', false);
                
                scrollTo(0);
                
                return false;
            });
        }

        if (filterGroup) {
            SearchEvents.getSelectedSearchView = top.SearchEvents.getSelectedSearchView;
            var view = SearchEvents.getSelectedSearchView();

            SearchEvents.getFieldValues = top.SearchEvents.getFieldValues;
            
            //window.ParametricFields = top.window.ParametricFields;
            window.FilterFields = top.window.FilterFields;
            window.SearchConfig = top.window.SearchConfig;
            window.FilterTypeOperators = top.window.FilterTypeOperators;
            
            
            window.FILTER_FIELDS_TYPE = top.window.FILTER_FIELDS_TYPE
            window.FILTER_FIELD_SELECTOR = top.window.FILTER_FIELD_SELECTOR;
            window.FIELD_OPERATOR_SELECTOR = top.window.FIELD_OPERATOR_SELECTOR;
            window.FILTER_FIELD_DATA = top.window.FILTER_FIELD_DATA;
            window.FILTER_OPERATORS = top.window.FILTER_OPERATORS;
            window.PARAMETRIC_FIELDTYPE = top.window.PARAMETRIC_FIELDTYPE;
            window.INDEXED_FIELDTYPE = top.window.INDEXED_FIELDTYPE;
            window.CUSTOM_FIELDTYPE = top.window.CUSTOM_FIELDTYPE;
            
            
            window.SearchSettings = null;

            var SNOMED_FIELD = SearchConfig.searchViews[view].snomedTag;
            var SNOMED_PARENT_FIELD = SearchConfig.searchViews[view].snomedParentTag;

            SearchEvents.setFilters = SearchEvents.attemptSearch = $.noop;
            SearchEvents.getDocumentLists = top.SearchEvents.getDocumentLists;
            SearchEvents.getNonRestrictedLists = top.SearchEvents.getNonRestrictedLists;
            SearchEvents.getRestrictedLists = top.SearchEvents.getRestrictedLists;

            SearchEvents.getParametricsXhr = _.memoize(function() {
                return $.ajax({
                    url : 'ajax/parametric/getParaFieldValues.json',
                    dataType : 'json',
                    data: {searchView: view }
                });
            });

            $filtersViewer.append('<form id="parametricForm" class="filters edit-disabled">');
            
            $('#parametricForm').on('mouseover,mousemove', 'legend', function() {
                return false;
            });
            
           
            var fieldlistControl = {};
            fieldlistControl.getFieldValues = top.SearchEvents.getFieldValues;
           
				
            //toggle check button in Doccview 
            var toggleCtr = false;
            $('#check').click(function(event){
            	toggleCtr = false;
				if($(this).val()=="Check All"){
					$("input[type='checkbox']").prop("checked", false);
					$(this).val('Uncheck All');
				}else if($(this).val()=="Uncheck All"){
					$("input[type='checkbox']").prop("checked", true);
					$(this).val('Check All');
				}
				$("input[type='checkbox']").trigger("click", function(){
				});
				$("#check").trigger("customCall",[callback]);
             });
            $("#check").bind("customCall", function(e, callback){
				callback();
			})
			
			var callback = function(){
            	toggleCtr = true;
            	$("#popuploader").show(0).delay(400).hide(0);
				}
			function callFuntion(index,item, currentthis ,test){
				
            	var g = jQuery.inArray(index, test)
            		if(g!=-1){
            			setTimeout(function() {
						currentthis.trigger("click");
						}, 80);
						}
					
				}
          //toggle check button in Doccview code end here
		    if(sessionStorage && sessionStorage.getItem("PreviousChecked") != 'null'){
			setTimeout(function() {
					var test = JSON.parse(sessionStorage.getItem("+ PreviousChecked +"));
					debugger;
					
					$("input[type='checkbox']").each(function (index, item) {
					//setTimeout(function() {
						var present = $(this);
						callFuntion(index,item,present,test);
					//}, 10);
					});
					
				}, 10);
			} 
			
            var $parametricForm = $('#parametricForm').filterForm({fieldlistControl: fieldlistControl}).on('click',  '.filter-item', 'input[type="checkbox"]', function(event) {
			var widgetArray = [];
			var itemArray = [];
			   var $filter = $(this);
			   var pullFilter =[];
			   
			   $(".peepCheck").each(function (index, itemvalue) {
				   
				   if(itemvalue.checked){
					   pullFilter.push(index);
				   }
				
			   })
			   
			   sessionStorage.setItem("+ PreviousChecked +", JSON.stringify(pullFilter));   
			   
			   $("input[type='checkbox']:checked").each(function (index, item) {
			   	if ($('input[type="checkbox"]').is(':checked')) {
						var a = new Date(item.value);
						if(a != "Invalid Date"){
							widgetArray.push($(this).parent().parent().parent().parent());
						}else{
							widgetArray.push($(this).parent().parent().parent());
						}
						
					}else{
						var textArea = $(this).closest('div').find('textarea').val(); 
					} 
				});
			    $("input:checkbox:not(:checked)").each(function(index,item){
					$("span[data-field]").removeClass("docquery-highlight");
					$('htag').removeClass('docquery-highlight');
					itemArray.push(item);
				})
				
				//based on individual check in Docview, change the Top CheckALL button 
			   if(itemArray.length!= 0 && toggleCtr){
				  $('#check').val("Check All");
			   }
			  //based on individual check in Docview, change the Top UnCheckALL button
			   if(itemArray.length== 0){
				    $('#check').val("Uncheck All");
				   
			   }else if(!toggleCtr){
				    $('#check').val("Check All");
			   }
			 //based on individual check in Docview, change the Top UnCheckALL button code end here
			 
			   if(widgetArray.length==0){
				   $("span[data-field]").removeClass("docquery-highlight");
				   var $typeField = $(this).closest('div').find('textarea').removeClass('hlclicked');
				    $(this).closest('div').find('input').removeClass('hlclicked');
				   toggleHlNavLink(null, true);
				}
			    
			  for(var h=0; h < widgetArray.length; h++ ){
			   var arrowTotalcount = [];
			   var $filter = widgetArray[h];
                var widgetType = $filter.data('widgetName');
                if (!widgetType) {
                    return
                }
                
                var widget = $filter.data(widgetType);
                var fieldMeta = widget.options.data;

                if (!fieldMeta) {
                    return;
                }
                
                var fieldType;
                
                switch(fieldMeta.fieldType) {
                    case PARAMETRIC_FIELDTYPE:
                        fieldType = fieldMeta.parametric.type;
                        break;
                    case INDEXED_FIELDTYPE:
                        fieldType = fieldMeta.indexed.type;
                        break;
                    case CUSTOM_FIELDTYPE:
                        fieldType = fieldMeta.custom.type;
                        break;
                        
                }    
                // we trim out all the leading ADMISSION and PATIENT slashes, if any, to standardize the
                // differences between the field mappings in the ADMISSIONS and PATIENTS database
                var IDOLfield = getFieldNameFromMeta(fieldMeta);
                
                 $('.hlclicked').removeClass('hlclicked');
                var $typeField = $(this).find('input[type="text"]');
                if($typeField.length>0){
                	$typeField.addClass('hlclicked');
					$(this).closest('div').find('input').removeClass('hlclicked');
                } else {
                $("span[data-field='"+fieldMeta.name+"']").removeClass("docquery-highlight")
                	}
               
                var mycheck = [];
                var checkedVal = "";
				$("input[type='checkbox']:checked").each(function (index, item) {
					if ($("input[type='checkbox']").is(':checked')) {
						// TEXT AREA
						if($(this).closest('div').find('textarea').val()){
							mycheck.push($(this).closest('div').find('textarea').val());
						} else { 
						// NORMAL TEXT FIELD
							var comma = ",";
							mycheck.push($(this).closest('div').find('input').val() + comma);
							var $typeField = $(this).closest('div').find('input').addClass('hlclicked');
						}
						var $typeField = $(this).closest('div').find('textarea').addClass('hlclicked');
					}
				}); 
			   mycheck = mycheck.join("");
			   checkedVal = mycheck.split("\n").join("^")
			   var singleValhighlight ;
			   singleValhighlight = fieldMeta.filterValue.split("\n")[0];
				var hlfield = {name: fieldMeta.name, value: singleValhighlight, type: FILTER_HL_TYPE};
                if (top.SearchSettings) {
                    top.SearchSettings.setDocviewHlField({field: hlfield});
                }
               
			    // Snomed field / concepts
                if (isSnomedField(fieldMeta.name)) {
                	debugger;
						$('#navtree').jstree("deselect_all");				
						// find Concept, or Concept+ in the map  
						var upSelectedConcept=fieldMeta.filterValue.split("\n");
						upSelectedConcept=upSelectedConcept.map(function(x) {
							return x.toLowerCase();
						})
						
						for (var tval in parentConceptListMap) {
							//var matchi=-1;
							//matchi=tval.toLowerCase().indexOf(concept.toLowerCase().trim())
							
							if(_.contains(upSelectedConcept, tval.toLowerCase())) {
								// get list of hids
								clist=parentConceptListMap[tval]
								// Walk list of concept tree nodes and select if hid is in clist 
								for (tid in hidMap) {
									thidlist=hidMap[tid];
									for (ithid in thidlist) {
										if (clist.indexOf(thidlist[ithid])>=0) {
											pNode= $('#navtree').jstree('select_node',tid);	
											$(pNode).parents(".jstree-open").each(function(index){
													var theParent=$(this);
													// Apply operation to each parent
													$('#navtree').jstree('select_node',theParent);
												});
										}
									}	
								}						
							}
						}
                	
                } else {
					// FIELDS
					//  setTimeout(function() {
                    var preprocessFn = PROCESSOR[fieldType] || function(a) {
						return a.toUpperCase().trim().replace(/\s+/g,' ');
                    }; 
					
					var metaValue = fieldMeta.filterValue.split('\n').join("^")
					var textareaValues = checkedVal+metaValue;
					
					textareaValues = Array.from(new Set(textareaValues.split(','))).join().toString();
					var lastChar = textareaValues.slice(-1);
					
					if (lastChar == '^') {
						textareaValues = textareaValues.slice(0, -1);
					}
					
					arrayValues = textareaValues.split('^');
					
					var i;
					var arrLength = arrayValues.length;
					arrayValues = arrayValues.filter(Boolean);
					for(i=0; i<arrLength; i++){
						
						if(i!=arrayValues.length){
							isMultiple = true
						}
						singleValhighlight = arrayValues[i];
						
						
						   var testValue = preprocessFn($.trim(singleValhighlight), widget);
						
						 //noinspection JSUnresolvedVariable
						   var field = FilterFields[view][fieldMeta.name];
		
							var compareFn = OP[fieldMeta.fieldOp] || field && field.type === 'range' && function(a, b){
								// range is implemented low < val <= high
								var aNum = Number(a);
								var bRange = b.split(/[,\u2192]/);
								var upperUnBounded = bRange.length < 2 || _.isEmpty(bRange[1]);
								
								return bRange.length === 2 && (upperUnBounded ? aNum > bRange[0] : aNum > bRange[0] && aNum <= bRange[1]);
							} || function(a, b){
								return a === b;
							};
							
								
							// DOCUMENT FOLDER
							if (fieldType === 'DOCUMENTFOLDER') {
								var tempVal = preprocessFn(String($(e).data('documentfolder').id), widget);
								var $match = $('.documentfolder').filter(function(i,e){
									return compareFn(tempVal, testValue);
								}).first();
		
								toggleHlNavLink(null, true);
		
								if ($match.length) {
									$('#folder-tags').animate({
										scrollTop: $match[0].offsetTop - 0.5 * $match.height()
									}, 300, function(){
										docqueryHighlight($match.find('span'));
									});
								}
							} else {
		
								// FIELD
								var SHORTField=IDOLfield.split("/_/").pop();
								
								var $potential = $('span[data-field$="'+ SHORTField +'"]');
									
								var $matched = $potential.filter(function(i, e){
									temVal = $(e).text().trim();
									textHtml = temVal.replace(/^"*|"*$/g, "");
									temVal = textHtml;
									return compareFn(preprocessFn(temVal, widget), testValue);
								});
								
								var $toHighlight = ($matched.length==0) ? null : $matched;
									
								if ($matched.length>0) {
									arrowTotalcount.push($matched);
									idx = 0;
									nHighlights = 1;
									docqueryHighlight($toHighlight);
								   // scrollTo(0);
								}
							} // end for
	                    }
						toggleHlNavLink(arrowTotalcount);
					
						/*if(arrowTotalcount.length>0){
							toggleHlNavLink(arrowTotalcount);
						}*/
						//scrollTo(0);
	                    if (top.SearchSettings.setActiveViewTab) {
	                        top.SearchSettings.setActiveViewTab('#' + $('.tab-pane.active').id);
	                    }
                    //}
				 // Arrow RightSide count 
              // },100) 
			    $('#rightHLBtn').trigger("click"); 
			   }
			
                    function flashHighlight($toHighlight) {
                    $toHighlight.stop().animate({backgroundColor: 'yellow'}, 50).delay(1000).animate({backgroundColor: 'transparent'}, 300, function(){
                        $toHighlight.css('backgroundColor', '')
                    })
                }
				
		 }
		  
            });

            var filtergroupTemplate = $.resGet('../templates/search/filters.filtergroup'+tempID+'.template');
            $parametricForm.append($(filtergroupTemplate));

            $('fieldset.filter-group').filtergroup({isRoot: true, fieldlistControl: fieldlistControl});

            $parametricForm.find('.rootFilterGroup').data('filtergroup').loadFilters(filterGroup);
            $parametricForm.find('select').prop('disabled', true);
            $parametricForm.find('input').prop('readonly', true);
            $parametricForm.find('textarea').prop('readonly', true);
        }
    }
    if (top.SearchEvents && top.SearchEvents.getDocuments) {
        var $resultNav = $('#result-nav');
        $resultNav.closest('li').removeClass('hide');

        function onDocsLoaded(){
		   var docs = top.SearchEvents.getDocuments();
            var idx = docs.idx;
            var urls = docs.urls;

            $resultNav.html(_.template('<span class="resultcount">Chart <%-idx+1%> out of <%-total ? total : urls.length + " visible"%></span>', {
                urls: urls,
                idx: idx,
                total: docs.total
            }));
            
            var lastActiveTab = top.SearchSettings && top.SearchSettings.getActiveViewTab();
            var isLastTabEq = lastActiveTab && lastActiveTab.substring(1) === $('.tab-pane.active').id;
            if (lastActiveTab && !isLastTabEq) {
               $navTabs.filter('a[href="' + lastActiveTab + '"]').click();
                
            }
                    
            var hlfieldData = (top.SearchSettings) ? top.SearchSettings.getDocviewHlField() : null;
            if (hlfieldData && hlfieldData.field.type === FILTER_HL_TYPE) {
                var $filterItems = $('#parametricForm .filter-item');
                $filterItems.each(function() {
                    var $filter = $(this);
                    var widgetType = $filter.data('widgetName');
                    if (widgetType) {
                        var widget = $filter.data(widgetType);
                        var fieldMeta = widget.options.data;
                        if (hlfieldData.field.name === fieldMeta.name && hlfieldData.field.value === fieldMeta.filterValue.trim()) {
                            setTimeout(function() {$filter.click();}, 10);
                            return false;
                        }
                    }
                });
            } else if (hlfieldData && hlfieldData.field.type === LINK_HL_TYPE) {
                var $toClick = $('a.highlight-link').filter(getConceptHlSelector(hlfieldData.field.value));
                if ($toClick.length > 0) {
                    conceptLinksHighlight($toClick);
                }
                
            } else if (hlfieldData && hlfieldData.field.type === QUERY_HL_TYPE) {
                setTimeout(function() {$('#queryTextInput').click();}, 10);
                return false;
            }
        }

        
        top.SearchEvents.$.on(top.SearchEvents.RESULTS_LOADED, onDocsLoaded);
        
       $(window).load(function(){
		//	onDocsLoaded();	   
	   })
	   
        $(window).unload(function(){
            top.SearchEvents && top.SearchEvents.$.off(top.SearchEvents.RESULTS_LOADED, onDocsLoaded);
        })
    }
    
    $('#leftHLBtn,#rightHLBtn').click(function(){
	
        if (!$(this).hasClass('disabled')) {
            var left = this.id === 'leftHLBtn';
            idx = (idx + nHighlights + (left ? -1 : 1)) % nHighlights;
            $('#currentHlLabel').text(idx + 1);
            scrollTo(idx);
        }
        
        return false;
    });

       
    function isSnomedField(fieldname) {
        return SNOMED_FIELD === fieldname || SNOMED_PARENT_FIELD === fieldname;
    }
    
    function isConcept(fieldname) {
        return SNOMED_FIELD === fieldname;
    }
    
    function isConceptPlus(fieldname) {
        return SNOMED_PARENT_FIELD === fieldname;
    }

    function scrollTo(idx) {
        var $highlights = $('.docquery-highlight');
		var $hightlightsFrame=$(".docview_iframe_content").contents().find('.docquery-highlight');
		$highlights=$highlights.add($hightlightsFrame);
        $highlights.filter('.active').removeClass('active');
        var $highlights2 = $('.docquery-highlight2');
        $highlights2.filter('.active').removeClass('active');
       
		var $toHighLight = $highlights.eq(idx).addClass('active');
        var hNum=$toHighLight.attr('hNum')
		if (hNum) {
			var peerSelector="[hNum="+hNum+"]"
			$(peerSelector).addClass('active');
		}
		
		
        nHighlights = $highlights.length;
		$('#totalHlLabel').text(nHighlights);
        scrollToEl($toHighLight, 0.5);
    }
		
	function getFrameElement($el) {
		var iframes = document.getElementsByTagName('iframe');
		var eldoc= $el[0].ownerDocument;
		for (var i= iframes.length; i --> 0;) {
			var iframe=iframes[i];
			try {
				var idoc= 'contentDocument' in iframe? iframe.contentDocument : iframe.contentWindow.document;				
			} catch (e) {
				continue;
			}
			if (idoc===eldoc)
				return iframe;
		}
	}
		
    function scrollToEl($el, fraction, skipAnimation, callback) {
        
		// Show tab
		
		if ($el.length==0)
			return;
		$tabpane=$el.closest('.tab-pane');
		if ($tabpane.length==0) {
			$iframe=$(getFrameElement($el));
			$tabpane=$iframe.closest('.tab-pane');
			$('a[href=#' + $tabpane[0].id + ']').tab('show');
		
			$el.is(':visible') || $el.closest('.admission').find('h4').siblings().show();
			
			// Scroll to iframe
			var curScroll=$container[0].scrollTop;
			var curHeight=$container.height();
			var hel=$iframe[0];
			var scrollTop = 0;
			while (hel!=null && hel!=$container[0]) {
				scrollTop=scrollTop+hel.offsetTop+ 250;
				hel=hel.offsetParent;    
			}
			scrollTop=scrollTop-fraction*$container[0].clientHeight;
			if (scrollTop<0) {
				scrollTop=0
			}
			if (skipAnimation) {
				$container[0].scrollTop = scrollTop;
				// callback has to be called asynchronously otherwise it'll be fired before the scroll() event is fired.
				callback && setTimeout(callback, 1);
			}
			else {
				$container.animate({
					scrollTop: scrollTop
				}, 300, undefined, callback);
			}
		
			// Scroll within iframe
			$
			var curScroll=$iframe.scrollTop;
			var $hel=$el[0];
			var scrollTop = 0;
			while ($hel!=null && $hel!=$iframe) {
				scrollTop=scrollTop+$hel.offsetTop;
				$hel=$hel.offsetParent;
			}
			scrollTop=scrollTop-fraction*$container[0].clientHeight;
			if (scrollTop<0) {
				scrollTop=0
			}
			var idoc= 'contentDocument' in $iframe[0]? $iframe[0].contentDocument : $iframe[0].contentWindow.document;				
			$body=$(idoc.getElementsByTagName('body')[0]);
			if (true) {
				$body.scrollTop = scrollTop;
				// callback has to be called asynchronously otherwise it'll be fired before the scroll() event is fired.
				callback && setTimeout(callback, 1);
			}
			else {
				$body.animate({
					scrollTop: scrollTop
				}, 300, undefined, callback);
			}
			
		
		
		
		
		} else {
			$('a[href=#' + $tabpane.attr('id') + ']').tab('show');
	
        // if the element is in an admissions block, expand the block so there's something to see
			$el.is(':visible') || $el.closest('.admission').find('h4').siblings().show();

			var curScroll=$container[0].scrollTop;
			var curHeight=$container.height();
			var $hel=$el[0];
			var scrollTop = 0;
			while ($hel!=null && $hel!=$container[0]) {
				scrollTop=scrollTop+$hel.offsetTop;
				$hel=$hel.offsetParent;
			}
			scrollTop=scrollTop-fraction*$container[0].clientHeight;
			if (scrollTop<0) {
				scrollTop=0
			}
			if (skipAnimation) {
				$container[0].scrollTop = scrollTop;
				// callback has to be called asynchronously otherwise it'll be fired before the scroll() event is fired.
				callback && setTimeout(callback, 1);
			}
			else {
				$container.animate({
					scrollTop: scrollTop
				}, 300, undefined, callback);
			}
		}
    }
    
    function docqueryHighlight($toHighlight, isActive) {
		/*if(!isMultiple){
			clearHighlight();
		}*/
        var hlClasses = isActive ? 'docquery-highlight active' : 'docquery-highlight';
        $toHighlight && $toHighlight.addClass(hlClasses);
    }
    
    function clearHighlight() {
    		if ($(HL_EMBEDDED_SRC_SELECTOR).is(':visible')) {
					$(HL_EMBEDDED_SRC_SELECTOR).toggleClass('hidden', true);
					$(HL_EMBEDDED_SELECTOR).toggleClass('hidden', false);
    		}
    		
        $('.docquery-highlight-link').removeClass('docquery-highlight-link');
        $('.docquery-highlight').removeClass('docquery-highlight active');
    }
    
    function toggleHlNavLink($toHighlight, hideLabel) {
	
    	var needHighlight = $toHighlight && $toHighlight.length;
        if (needHighlight || !hideLabel) {
            $('#leftHLBtn,#rightHLBtn').toggleClass('disabled', !needHighlight);
            var currentCount = needHighlight ? 1 : 0;
            var totalCount = needHighlight ? $toHighlight.length : 0;
            $('#currentHlLabel').text(currentCount);
            $('#hlLabel').text('of');
			if($('#totalHlLabel').text()){
				
				var curTotalCount = Number($("span.docquery-highlight").length) + Number($("htag.docquery-highlight").length);
				//var curTotalCount = Number($('#totalHlLabel').text());
				var ttotal = curTotalCount+totalCount;
				$('#totalHlLabel').text(curTotalCount);
			}else{
				$('#totalHlLabel').text(totalCount);
			}
            
            
            $('div.hl-nav-label').toggleClass('zero-hit', !totalCount && !hideLabel);
			if($('#totalHlLabel').text()!="0"){
				 $('div.hl-nav-label').removeClass('zero-hit');
			}
			$('#leftHLBtn,#rightHLBtn').removeClass('disabled', true);
			if($('#currentHlLabel').text() == "0" && $('#totalHlLabel').text() != "0"){
				//alert("CurrentLabel")
			$('#currentHlLabel').text("1")
			}
			
        } else {
			$('#leftHLBtn,#rightHLBtn').toggleClass('disabled', true);
            $('#currentHlLabel').text('');
            $('#hlLabel').text('\u00A0');
            $('#totalHlLabel').text('');
        }
        
    }
	
    function conceptLinksHighlight($toHighlight) {              
     //   clearHighlight();
        idx = 0;
        
        var $concepts = null;
        var conceptSelectors = [];
        
        if ($toHighlight && $toHighlight.length) {
            $toHighlight.addClass('docquery-highlight-link');
            // ensure the SNOMED supercategory for the term is visible
            $toHighlight.is(':visible') || $toHighlight.closest('div').prevAll('h3:first').nextUntil('h3').show();
            
            $fieldList.animate({
                scrollTop: $toHighlight[0].offsetTop - 0.5 * $fieldList.height()
            }, 300);
            
            
            // highlight docs
            $toHighlight.each(function() {
                var cid = $(this).attr('name');
                conceptSelectors.push('htag[l="' + $(this).attr('name') + '"]');
            })
            
            $concepts = $(conceptSelectors.join());
            
            $concepts.addClass('docquery-highlight');

            if ($toHighlight.length > 1) {
                $concepts.each(function() {
                    var $concept = $(this);
                    
                    var hlTerm = $concept.text();
                    var $children = $concept.find('> .docquery-highlight');
                    while ($children.length > 0) {
                        var $child = $children.first();
                        if ($child.text() === hlTerm) {
                            $child.removeClass('docquery-highlight');
                            $children = $child.find('> .docquery-highlight');
                        } else {
                            break;
                        }
                    }
                    
                });
                
                toggleHlNavLink($concepts.filter('.docquery-highlight'));
                
            } else {
                toggleHlNavLink($concepts);
                
            }
            
            
            scrollTo(idx);
            
        } else {
            $('.docquery-highlight-link').removeClass('docquery-highlight-link');
            toggleHlNavLink($toHighlight);
        }
   
    }
        
    function getConceptHlSelector(conceptId) {
        return '[name="' + conceptId + '"]';
    }
    
    function getChildConceptNodes(parentId, childNodesArr) {
        var parentSelector = CONCEPT_METATAG + '[' + CONCEPT_ID_ATTR + '="' + parentId + '"]';
        var childSelector = '[' + CONCEPT_CHILDID_ATTR + ']';
        
        childNodesArr.push(getConceptHlSelector(parentId));
        
        var $childNodes = $(parentSelector).filter(childSelector);
        $childNodes.each(function() {
            getChildConceptNodes($(this).attr(CONCEPT_CHILDID_ATTR), childNodesArr);
        });
        
        return childNodesArr;
        
    }
    
    function getFieldNameFromMeta(fieldMeta) {
        var fieldName = null;
        switch(fieldMeta.fieldType) {
            case PARAMETRIC_FIELDTYPE:
                fieldName = fieldMeta.parametric.name;
                break;
            case INDEXED_FIELDTYPE:
                fieldName = fieldMeta.indexed.name;
                break;
            case CUSTOM_FIELDTYPE:
                fieldName = fieldMeta.custom.name;
                break;
            default:
                fieldName = fieldMeta.name;
        }
        
        return fieldName || fieldMeta.name;
        
        
    }
    function myFunction() {
        document.getElementById("myDropdown").classList.toggle("show");
    }
	
	 if($('#parametricForm').length==1){
		$('input[type="button"]').show()
		}else{
			$('input[type="button"]').hide()
		}   
	
 // Close the dropdown if the user clicks outside of it
    window.onclick = function(event) {
    	matches = event.target.matches ? event.target.matches('.dropbtn') : event.target.msMatchesSelector('.dropbtn');
    		if (!matches) {
		        var dropdowns = document.getElementsByClassName("dropdown-content");
		        var i;
		        for (i = 0; i < dropdowns.length; i++) {
		          var openDropdown = dropdowns[i];
		          if (openDropdown.classList.contains('show')) {
		            openDropdown.classList.remove('show');
		          }
		        }
    		}
    	
    } 
  
    $("htag").click(
    		  function () {
    			  if (!$(this).hasClass("docquery-highlight")) {
    				  return;
    			  }
    			  var pos=$(this).offset();
    			  var height=$(this).height();    			 
    			  // get value of tag
    			  sstr=$(this).text();
    			  
    			  
    			  // modify link content 
    			  // clink1="http://www.omim.org/search?index=geneMap&search="+sstr+"&start=1&limit=10";
    			  clink1="http://www.ncbi.nlm.nih.gov/omim/?term="+sstr;
    			  $("#OMIMGM_TERM_SEARCH").attr('href',clink1);    			  
    			  
    			  clink2="http://www.ncbi.nlm.nih.gov/pubmed/?term="+sstr;
    			  $("#PUBMED_TERM_SEARCH").attr('href',clink2);    			  
    			  
    			  
    			  
    			  // move menu list to location
      			  $("#link-menu").show();
      			  $("#link-menu").offset({ top: pos.top+ height+5, left: pos.left})
    			  
    			// show
    			// $("#link-menu").display = 'block';
     			  
    			//  $('ul.file_menu').slideDown('medium');
    		  }
    		);

	  $("#link-menu").mouseleave(
	  		function () {
		  //   $('ul.file_menu').slideUp('medium');
		  var pos=$(this).offset();
		  console.log("hover out "+ pos.top + " "+ pos.left);
		  // hide menu list
		  $(this).hide();
	  	})

	  	$("#link-menu").hide();
    
});