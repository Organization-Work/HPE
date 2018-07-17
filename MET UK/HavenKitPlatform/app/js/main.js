
/** Login Function **/
/**
function buildContent(data) {
    var content = '<ul class="list-group" style="height:400px;overflow:auto">';

    for (var idx = 0; idx < data.length; idx++) {
        content += '<li class="list-group-item hp-item" onclick="hpSingle(this)" data-index="' + idx + '"><input type="checkbox" class="option" value="' + data[idx] + '" id="item' + idx + '"/>&nbsp;&nbsp;' + data[idx] + "</li>";
    }
    content += "</ul>";
    return content;
}
function hpSingle(item) {
    var attr = $(item).attr('data-index');
    var box = $("#item" + attr);
    box.prop("checked", !box.prop("checked"));
}
 function hpSelect() {
    $.ajax({
        type: "GET",
        url: "/select",
        dataJson: 'json',
        success: function (data) {
            var html = buildContent(data);
            BootstrapDialog.show({
                title: "IDOL Files Selection...",
                message: html,
                buttons: [{
                    label: "Select",
                    cssClass: 'btn-primary',
                    action: function (dialog) {
                        hpExecute();
                        dialog.close();
                    }
                }, {
                    label: "Cancel",
                    action: function (dialog) {
                        dialog.close();
                    }
                }]
            });
        }
    })
}
function hpExecute() {
    var items = $('.option:checked');
    var data = [];
    for (var idx = 0; idx < items.length; idx++) {
        data.push($(items[idx]).val());
    }
    // Store Data
    $('#hdData').val(JSON.stringify(data));
}
**/
function initButtonStatus() {
    //$("#browse").attr("disabled", false);
    //$("#start-upload").attr("disabled", true);
    //$("#btnAnalyze").attr("disabled", true);
    //console.log("initButtonStatus");
}

function hpAnalyzeUI(id) {
    var pbar = $("#pbar" + id);
    pbar.addClass('progress');
    pbar.html('<div class="progress-bar progress-bar-striped active" data-transitiongoal="75"/></div>');
    var bar = pbar.find('.progress-bar');
    bar.addClass('progress-bar');
    bar.css('color', 'black');
    bar.css('text-align', 'center');
    bar.text("0%");
}
// calculate process of each of tasks
var priorityValues = {};

function calPercent(id, recordCount, currentStatus)
{
    var pbartext = $("#pbartext" + id);
    pbartext.text(currentStatus);
	
    if ("Finished"  == currentStatus) {
	  $("#stopProcess" + id).hide();
	  $("#priorityProcess" + id).hide();
	  $("#startProcess" + id).hide();
	  $("#next" + id).show();
	  
	  $('#priorityTextBox' + id).hide();
      pbartext.css('color', '#5cb85c');
      pbartext.text("Complete");
	  hpExtractionFinished(id);
      return 100;
    }
    else if ("Processing" == currentStatus) {
	  $("#stopProcess" + id).show();
	  $("#priorityProcess" + id).hide();
	  $("#startProcess" + id).hide();
	  $("#trimProcess" + id).hide();
	  $('#priorityTextBox' + id).hide();
      var token = JSON.parse($("#hdToken").val());
      pbartext.css('color', '#265a88');
      return (100*recordCount/token[id].currentVideoTotalFrames).toFixed(0).toString();
    }
    else if ("Queued" == currentStatus) {
	  $("#stopProcess" + id).hide();
	  $("#priorityProcess" + id).show();
	  $("#startProcess" + id).hide();
	  $("#trimProcess" + id).hide();
	  $('#priorityTextBox' + id).show();
	  $('#priorityTextBox' + id).attr("disabled",false);
	  var priorityValue = JSON.parse(localStorage.getItem("priorityValuesSession"))[id];
	  $('#priorityTextBox' + id).attr("value",priorityValue);
	  pbartext.text(currentStatus+" "+priorityValue);
	  //change the color if priority is set gerater than 0
	  if(priorityValue>0 && priorityValue<=100){
		  pbartext.css('color', '#0073e7');
	  }
	  else{
		  pbartext.css('color', 'black');
	  }
      return 0;
    }
	else if('stoped' == currentStatus){
		$("#stopProcess" + id).hide();
	    $("#priorityProcess" + id).hide();
	    $("#startProcess" + id).show();
		$('#priorityTextBox' + id).hide();
        pbartext.css('color', 'red');
		
        var data = $("#hdTime").val();
        if(data){
            data = JSON.parse(data);
            var intervalId = data[id];
            clearInterval(intervalId);
        }
        return 0;
	}
    else {
        pbartext.css('color', 'red');
        remove(id,"hdToken");
        if ("{}" == $("#hdToken").val() || "" == $("#hdToken").val()){
            initButtonStatus();
        }
        var data = $("#hdTime").val();
        if(data){
            data = JSON.parse(data);
            var intervalId = data[id];
            clearInterval(intervalId);
            remove(id,"hdTime");
        }
        return 0;
    }
}

function hpProgress(id, value) {
    var recordCount = value.recordCount;
    var currentStatus = value.currentStatus;
    
    console.log("hpProgress:"+id+","+recordCount+","+currentStatus);
	if("Processing" == currentStatus){
		if(recordCount == 0){
			return;
		}
	}
	/*
    if (0 == recordCount && currentStatus == "Processing") {
        //console.log("exceptional update, skip to refresh the progress bar.");
        return;
    }*/
	$('#myLoaderModal').modal('hide');
    var percent = calPercent(id,recordCount,currentStatus);

    var pbar = $("#pbar" + id);
    var bar = pbar.find('.progress-bar');
    bar.css('color', 'white');
    if(100 <= percent){
        bar.css('width', "100%");
        bar.removeClass('progress-bar-primary');
		bar.removeClass('active');
        bar.addClass('progress-bar-success');
        bar.text('100%');
		$("#next" + id).show();
		$("#stopProcess" + id).hide();
		$("#pbartext" + id).text("Complete");
		$("#pbartext" + id).css('color', '#5cb85c');
		
        var data = $("#hdTime").val();
        if(data){
            data = JSON.parse(data);
            var intervalId = data[id];
            clearInterval(intervalId);
            //remove(id,"hdTime");
            //remove(id,"hdToken");
            if ("{}" == $("#hdToken").val() || "" == $("#hdToken").val()){
                initButtonStatus();
            }
        }
    }
    else {
        bar.css('width', percent + "%");
        bar.text(percent + '%');
    }
}

function hpWatcher(id) {
    return function () {
        if ("" == $("#hdToken").val()) {
            //console.log("wait a while");
        }
        else {
            var token = JSON.parse($("#hdToken").val());
			localStorage.setItem('sessionTokenObjTemp', JSON.stringify(token));
            //console.log("currentToken:" + token[id].currentToken + ",id:" + id);
            $.ajax({
                type: "GET",
                url: "/checkProgress",
                data: {
					Token :  token[id].currentToken,
					currentVideoTotalFrames: token[id].currentVideoTotalFrames
				},
                dataJson: 'json',
                success: function (data) {
					//console.log(data);
                    hpProgress(id,data);
                },
                error : function(jqXHR, textStatus, errorThrow) {
                    var pbartext = $("#pbartext" + id);
                    pbartext.css('color', 'red');
                    pbartext.text("Error");
                    console.log(jqXHR.responseText);
                    console.log(jqXHR.status);
                    console.log(jqXHR.readyState);
                    console.log(jqXHR.statusText);
                    console.log(textStatus);
                    console.log(errorThrown);
                    remove(id,"hdToken");
                    if ("{}" == $("#hdToken").val() || "" == $("#hdToken").val()){
                        initButtonStatus();
                    }
                    var data = $("#hdTime").val();

                    if(data){
                        data = JSON.parse(data);
                        var intervalId = data[id];
                        clearInterval(intervalId);
                        remove(id,"hdTime");
                    }
                }
            })
        }
    }
}

// To start any stoped extraction process
function hpExtractionStart(id){
	var data = JSON.parse($("#hdData").val());
	hpRequest(id, data[id]);
}

// To stop any extraction process
function hpExtractionStop(id){
	var token = JSON.parse($("#hdToken").val());
	$.ajax({
        type: "GET",
        url: "/stop",
        data: 'Token=' + token[id].currentToken,
        dataJson: 'json',
        success: function (data) {
			if(data.Error){
				alert('Failed to stop, check logs for more details');
			}
			else{
				hpProgress(id,data);
			}
        },
        error : function() {
			alert('Failed To Stop');
        }
    })
}

//for closing displayFaces or allFaces
function closeFaces(id){
	$('#filelist').find('#displayAllFacesContainer'+id).remove();
	$('#filelist').find('#displayFacesContainer'+id).remove();
	$("#displayMetaData" + id).hide();
	$("#next" + id).show();
}
function closeMeta(id){
	$('#filelist').find('#MetaDataContainer'+id).remove();
	$("#displayMetaData" + id).hide();
	$("#next" + id).show();
} 

// To display all crop faces
function displayFaces(id){
	var token = JSON.parse($("#hdToken").val());
	$.ajax({
        type: "GET",
        url: "/faces",
		data: {
 			Token :  token[id].currentToken
 		},
        dataJson: 'json',
        success: function (data) {
			if(data.Error){
				alert('Failed to display faces, check logs for more details');
			}
			else{
			if(data.Path.length!=0){
			$('#filelist').find('#displayFacesContainer'+id).remove();
			var displayFacesHtml = '';
			var count = 0;
			var dropBoxDirPathLength=data.dropBoxPath.length;
			var dropBoxPath= data.dropBoxPath.replace(/\//g, "\\");
			for(var a in data.Path){
				var fullPath = (data.Path[a]);
				//var trimPath = fullPath.substr(48, fullPath.length-1);
				var trimPath = fullPath.substr(dropBoxDirPathLength, fullPath.length-1);
				//console.log(trimPath);
				var checkImageExist = fileExists(trimPath);
				//faceID = 'face_'+ token[id].currentToken;
				faceID = 'face_'+ data.recordId[a];
				//console.log('faceID: '+ data.recordId[a]);
				
				if(checkImageExist==true){
					var escapedFullPath = fullPath.replace(/\\/g, "/")
					//console.log(escapedFullPath);
					var directoryPath = escapedFullPath.substring(0, escapedFullPath.lastIndexOf("/"))
					//console.log(directoryPath);
					displayFacesHtml += '<div style="cursor:pointer" class="img-wrap" id='+faceID+'><input id="selectFace" type="checkbox" class="uniqueFacesCheckBox" value=""><a oncontextmenu="openFileExplorerSingle(\'' + escapedFullPath + '\');return false;" onClick="displayMetaData(\'' + id + '\',\''+ data.recordId[a] + '\')"><img src='+ trimPath +' height="169" width="169" alt="Faces" style="margin-left:10px; margin-top:8px; border: 1px solid #ddd; border-radius: 4px; padding: 5px;"></a></div>'
					count++;
				}
			}
			if(count){
			$('#filelist').find('#'+id).after('<li id="displayFacesContainer' + id + '" class="list-group-item"><div class="container-face-heading"><span class="container-heading">Cropped faces</span><span class="glyphicon glyphicon-upload btn-lg" onClick="closeFaces(\'' + id + '\')" style="cursor:pointer;float:right; margin-top:-8px; margin-right:-27px"></span></div><div style="margin-left:10px">'+displayFacesHtml+'</div><div id="deleteExportContainer"><div style="margin-left:10px" class="btn-group deleteExportGroup"><a type="button" onClick="deleteSelectedFaces(\'' + id + '\',\''+ data.dropBoxPath + '\',\''+ 1 + '\')" class="btn mf-button-blue btn-xs">Delete <small><span class="glyphicon glyphicon-trash"></span></small></a><a type="button" onclick="exportSelectedFaces(\'' + id + '\',\''+ data.dropBoxPath + '\',\''+ 1 + '\')" class="btn mf-button-blue btn-xs">Export <small><span class="glyphicon glyphicon-export"></span></small></a></div><span id="alertExportMessage"></span></div></li>');
			
			$('#filelist').find('#MetaDataContainer'+id).remove();
			$('#filelist').find('#displayAllFacesContainer'+id).remove();
			}
			else{
				$('#filelist').find('#MetaDataContainer'+id).remove();
				$('#filelist').find('#displayAllFacesContainer'+id).remove();
				$('#filelist').find('#displayFacesContainer'+id).remove();
				$('#filelist').find('#'+id).after('<li id="displayFacesContainer' + id + '" class="list-group-item"><div class="container-face-heading"><span class="container-heading">Crop faces</span><span class="glyphicon glyphicon-upload btn-lg" onClick="closeFaces(\'' + id + '\')" style="cursor:pointer;float:right; margin-top:-8px; margin-right:-27px"></span></div><div><p>All faces has been deleted</p></div></li>');
			}
			}
			
			else{
				$('#filelist').find('#displayFacesContainer'+id).remove();
				$('#filelist').find('#'+id).after('<li id="displayFacesContainer' + id + '" class="list-group-item"><div class="container-face-heading"><span class="container-heading">Crop faces</span><span class="glyphicon glyphicon-upload btn-lg" onClick="closeFaces(\'' + id + '\')" style="cursor:pointer;float:right; margin-top:-8px; margin-right:-27px"></span></div><div><p>No Faces Detected</p></div></li>');
			}
			
			$('#displayFacesContainer'+id).hide().fadeIn('slow');
			}
        }
    })
}

function displayMetaData(id, recordId){
	var token = JSON.parse($("#hdToken").val());
    //console.log("Id from the onclick" +id+ "Record Id: " +recordId);
    var html = '';
    $.ajax({
        type: "GET",
        url: "/displayMetaData",
        data: {
            Token :  token[id].currentToken,
			recordId : recordId
        },
        dataJson: 'json',
        success:function (data) {
		if(data.Error){
			alert('Failed to display meta data, check logs for more details');
		}
		else{
		var dropBoxDirPathLength=data.dropBoxPath.length;
		var displayMetaDataHtml = '';
		for(a=0; a<data.cropPath.length; a++){
        //console.log("cropPath :" +data.cropPath[a]);
        //console.log("Full Frame :" +data.fullFrame[a]); 
        cpath = data.cropPath[a];
		var escaped_cPath = cpath.replace(/\\/g, "/")
		//console.log('cpath: '+cpath);
        //croPpath = cpath.substr(48, cpath.length-1);
		croPpath = cpath.substr(dropBoxDirPathLength, cpath.length-1);
        fFrame = data.fullFrame[a];
        vidpath = fFrame.replace("_full.jpg", ".mp4");
        txtpath = croPpath.replace("_crop.jpg", ".txt");
        //vidpath1 = vidpath.substr(48, vidpath.length-1);
		vidpath1 = vidpath.substr(dropBoxDirPathLength, vidpath.length-1);
        //fullFrame = fFrame.substr(48, fFrame.length-1);
		fullFrame = fFrame.substr(dropBoxDirPathLength, fFrame.length-1);
		
		displayMetaDataHtml+='<div class="metaResultsContainer" oncontextmenu="openFileExplorerSingle(\'' + escaped_cPath + '\');return false;"><div class="videoContainer"><input id="" type="checkbox" class="" style="position:absolute;z-index:1;" value=""><video src='+ vidpath1 +' height="362px" width="575px" style="" controls="" preload="auto"></video></div><div class="faceMetaContainer"><div class="faceContainer"><div class="cropFaceContainer"><input type="checkbox" id="metaCropFaceCheckBox" style="position:absolute" value=""><a title="Click to display all faces folder" class="metaCropFace"style="cursor:pointer;margin-right: 15px;" onClick="displayAllFaces(\'' + id + '\',\''+ data.recordId[a] + '\',\''+ recordId + '\')"><img src='+ croPpath +' height="240" width="240" alt="cropFace" style="border: 1px solid #ddd; border-radius: 4px; padding: 5px; vertical-align:top;"></a></div><div class="fullFrameContainer"><input id="metaFullframeCheckbox" type="checkbox" class="" style="position:absolute" value=""><img id="fullFrameImage" src='+ fullFrame +' height="240" width="240" alt="fullFrame" style="border: 1px solid #ddd; border-radius: 4px; padding: 5px; vertical-align:top"></div></div><div class="metaContainer"><input id="" type="checkbox" style="position:absolute" class="" value=""><iframe src='+ txtpath +' style="width: 498px;height: 105px;"></iframe></div></div></div>'
		
		}
		$('#filelist').find('#'+id).after('<li id="MetaDataContainer' + id + '" class="list-group-item"><div class="container-face-heading"><span class="glyphicon glyphicon-arrow-left btn-lg" onClick="displayFaces(\'' + id + '\')" style="cursor:pointer;float:left; margin-top:-8px; margin-left:-27px"></span><span class="container-heading">Meta data</span><span class="glyphicon glyphicon-upload btn-lg" onClick="closeMeta(\'' + id + '\')" style="cursor:pointer; float:right; margin-top:-8px; margin-right:-27px"></span></div>' + displayMetaDataHtml + '<div id="deleteExportContainer"><div class="btn-group deleteExportGroup"><a type="button" onclick="exportSelectedFaces(\'' + id + '\',\''+ data.dropBoxPath + '\',\''+ 2 + '\')" class="btn mf-button-blue btn-xs">Export <small><span class="glyphicon glyphicon-export"></span></small></a></div><span id="alertExportMessage"></span></div></li>');
		
		$('#MetaDataContainer'+id).hide().fadeIn('slow');
		
		$('#filelist').find('#displayFacesContainer'+id).remove();
		$('#filelist').find('#displayAllFacesContainer'+id).remove();
		$("#displayMetaData" + id).hide();
		$("#next" + id).show();
		}
		}
    })
}

function displayAllFaces(id, recordId, recordIdTemp){
	var token = JSON.parse($("#hdToken").val());
	$.ajax({
        type: "GET",
        url: "/allFaces",
		data: {
 			Token :  token[id].currentToken,
			recordId : recordId
 		},
        dataJson: 'json',
        success: function (data) {
			if(data.Error){
				alert('Failed to all faces, check logs for more details');
			}
			else{
			if(data.Path.length!=0){
			var displayFacesHtml = '';
			var count = 0;
			var dropBoxDirPathLength=data.dropBoxPath.length;
			for(var a in data.Path){
				var fullPath = (data.Path[a]);
				//console.log(fullPath);
				//console.log(data.recordId[a]);
				//var trimPath = fullPath.substr(48, fullPath.length-1);
				var trimPath = fullPath.substr(dropBoxDirPathLength, fullPath.length-1);
				//console.log(trimPath);
				var escapedFullPath = fullPath.replace(/\\/g, "/")
				var checkImageExist = fileExists(trimPath);
				//console.log(escapedFullPath);
				if(checkImageExist==true){
				faceID = 'face'+ count;
				displayFacesHtml += '<div class="img-wrap-allfaces" id='+faceID+'><input id="selectFace" type="checkbox" class="AllFacesCheckBox" value=""><a  oncontextmenu="openFileExplorerSingle(\'' + escapedFullPath + '\');return false;"><img src='+ trimPath +' height="164" width="164" alt="Faces"></a></div>'
				count++;
				}
			}
			if(count){
				$('#filelist').find('#'+id).after('<li id="displayAllFacesContainer' + id + '" class="list-group-item"><div class="container-face-heading"><span class="glyphicon glyphicon-arrow-left btn-lg" onClick="displayMetaData(\'' + id + '\',\''+ recordIdTemp + '\')" style="cursor:pointer;float:left; margin-top:-8px; margin-left:-27px"></span><span class="container-heading">All faces</span><span class="glyphicon glyphicon-upload btn-lg" onClick="closeFaces(\'' + id + '\')" style="cursor:pointer;float:right; margin-top:-8px; margin-right:-27px"></span></div><div>'+displayFacesHtml+'</div><div id="deleteExportContainer"><div class="btn-group deleteExportGroup"><a type="button" onClick="deleteSelectedFaces(\'' + id + '\',\''+ data.dropBoxPath + '\',\''+ 3 + '\')" class="btn mf-button-blue btn-xs">Delete <small><span class="glyphicon glyphicon-trash"></span></small></a><a type="button" onclick="exportSelectedFaces(\'' + id + '\',\''+ data.dropBoxPath + '\',\''+ 3 + '\')" class="btn mf-button-blue btn-xs">Export <small><span class="glyphicon glyphicon-export"></span></small></a></div><span id="alertExportMessage"></span></div></li>');
			
				$('#filelist').find('#MetaDataContainer'+id).remove();
			}
			else{
				$('#filelist').find('#MetaDataContainer'+id).remove();
				$('#filelist').find('#displayAllFacesContainer'+id).remove();
				$('#filelist').find('#displayFacesContainer'+id).remove();
				$('#filelist').find('#'+id).after('<li id="displayAllFacesContainer' + id + '" class="list-group-item"><div class="container-face-heading"><span class="glyphicon glyphicon-arrow-left btn-lg" onClick="displayMetaData(\'' + id + '\',\''+ recordIdTemp + '\')" style="cursor:pointer;float:left; margin-top:-8px; margin-left:-27px"></span><span class="container-heading">All faces</span><span class="glyphicon glyphicon-upload btn-lg" onClick="closeFaces(\'' + id + '\')" style="cursor:pointer;float:right; margin-top:-8px; margin-right:-27px"></span></div><div><p>All faces has been deleted</p></div></li>');
				}
			}
			else{
				$('#filelist').find('#displayAllFacesContainer'+id).remove();
				$('#filelist').find('#'+id).after('<li id="displayAllFacesContainer' + id + '" class="list-group-item"><div class="container-face-heading"><span class="glyphicon glyphicon-arrow-left btn-lg" onClick="displayMetaData(\'' + id + '\',\''+ recordIdTemp + '\')" style="cursor:pointer;float:left; margin-top:-8px; margin-left:-27px"></span><span class="container-heading">All faces</span><span class="glyphicon glyphicon-upload btn-lg" onClick="closeFaces(\'' + id + '\')" style="cursor:pointer;float:right; margin-top:-8px; margin-right:-27px"></span></div><div><p>No Faces Detected</p></div></li>');
			}
			$('#displayAllFacesContainer'+id).hide().fadeIn('slow');
			}
        }
    })
}

function deleteSelectedFaces(id, dropBoxPath, checkFaceWindow){
	var faceIdList = [];
	var faceSrcList = [];
	var hostname = window.location.origin;
	if(checkFaceWindow==1){
		$('#displayFacesContainer'+id+' > div:nth-child(2) > div.img-wrap> input:checked').each(function(){
			faceIdList.push(this.parentElement.id);
			var facePath = this.nextSibling.children[0].src;
			facePath=facePath.replace(hostname,"")
			facePath=facePath.substring(0, facePath.lastIndexOf("/"))
			faceSrcList.push(dropBoxPath+facePath);
		});
	
		if(faceSrcList.length){
			for(var i=0; i<faceSrcList.length; i++)
				deleteFaces(faceSrcList[i],faceIdList[i],id);
		}
		else{
			alert('None is selected');
			//$('.deleteExportGroup').after('<span id="deletionAlert">None is selected</span>');
			//$('#deletionAlert').fadeIn('slow').delay(5000).fadeOut('slow');
		}
	}
	
	else if(checkFaceWindow==3){
		$('#displayAllFacesContainer'+id+' > div:nth-child(2) > div.img-wrap-allfaces > input:checked').each(function(){
			faceIdList.push(this.parentElement.id);
			var facePath = this.nextSibling.children[0].src;
			facePath=facePath.replace(hostname,"");
			faceSrcList.push(dropBoxPath+facePath);
		});
		
		if(faceSrcList.length){
			for(var i=0; i<faceSrcList.length; i++)
				deleteFaces(faceSrcList[i],faceIdList[i],id);
		}
		else{
			alert('None is selected');
		}
	}
}

//For deleting faces
function deleteFaces(fullPath, faceId, id){
	
	$.ajax({
        type: "GET",
        url: "/deleteFaces",
        data: {
 			fullFacePath : fullPath
 		},
        dataJson: 'json',
        success: function (data) {
			$('#'+faceId).fadeOut('slow', function(){ 
				$('#'+faceId).remove(); 
				
				if(!($('#displayFacesContainer'+id+' img')).length){
					$('#filelist').find('#displayFacesContainer'+id).remove();
				}
				if(!($('#displayAllFacesContainer'+id+' img')).length){
					$('#filelist').find('#displayAllFacesContainer'+id).remove();
				}
			});
			$('#alertExportMessage').html('');
        },
        error : function() {
			alert('Failed to delete selected faces, Check the log file for more details');
        }
    })
}

function fileExists(url) {
    if(url){
        var req = new XMLHttpRequest();
        req.open('GET', url, false);
        req.send();
        return req.status==200;
    } else {
        return false;
    }
}


//For Exporting Images
function exportSelectedFaces(id, dropBoxPath, checkFaceWindow){	
	var faceIdList = [];
	var faceSrcList = [];
	var hostname = window.location.origin;
	
	if(checkFaceWindow==1){
		$('#displayFacesContainer'+id+' > div:nth-child(2) > div.img-wrap> input:checked').each(function(){
			faceIdList.push(this.parentElement.id);
			var facePath = this.nextSibling.children[0].src;
			facePath=facePath.replace(hostname,"");
			faceSrcList.push(dropBoxPath+facePath);
		});
		
		if(faceSrcList.length){
			exportFaces(faceSrcList,faceIdList,id,checkFaceWindow);
		}
		else{
			alert('None is selected');
		}
	}
	else if(checkFaceWindow==2){
		$('#MetaDataContainer'+id+' input:checked').each(function(){
			var facePath;
			if(this.id=="metaCropFaceCheckBox"){
				facePath=this.nextElementSibling.children[0].src;
				if(facePath.indexOf('matched_')!=-1){
					var faceName = facePath.substring(facePath.lastIndexOf('/')+1,facePath.lastIndexOf('_'));
					faceName = faceName.replace('matched_','');
					faceName = faceName.substring(0, faceName.lastIndexOf("_"));
					
					facePath = facePath.substring(0, facePath.lastIndexOf("/"));
					facePath = facePath.substring(0, facePath.lastIndexOf("/"));
					facePath = facePath+'/'+faceName+'/';
				}
				else{
					facePath=facePath.substring(0, facePath.lastIndexOf("/"))+'/'/*+'/allFaces'*/;
				}
			}
			else{
				facePath = this.nextElementSibling.src;
			}
			facePath=facePath.replace(hostname,"");
			faceSrcList.push(dropBoxPath+facePath);
		});
		
		if(faceSrcList.length){
			exportFaces(faceSrcList,faceIdList,id,checkFaceWindow);
		}
		else{
			alert('None is selected');
		}
	}
	
	else if(checkFaceWindow==3){
		$('#displayAllFacesContainer'+id+' > div:nth-child(2) > div.img-wrap-allfaces > input:checked').each(function(){
			faceIdList.push(this.parentElement.id);
			var facePath = this.nextSibling.children[0].src;
			facePath=facePath.replace(hostname,"");
			faceSrcList.push(dropBoxPath+facePath);
		});
		
		if(faceSrcList.length){
			exportFaces(faceSrcList,faceIdList,id,checkFaceWindow);
		}
		else{
			alert('None is selected');
		}
	}
}

function exportFaces(faceSrc, faceId, id,checkFaceWindow){
	var data = JSON.parse($("#hdData").val());
	var videoName = data[id];
	videoName = videoName.split(".")[0];
	
	var d = new Date();
	var n = d.toLocaleString();
	n = n.split('/').join('-').split(':').join('-').split(', ').join(',');
	
	var folderName = videoName+','+n;
	
	
	$("#exportFolderInput").val(folderName);
	$('#exportFolderModal').modal('show');
	
	$('#exportFolderNameSubmit').on('click', function(){
			$.ajax({
			type: "GET",
			url: "/exportFaces",
			data: {
				facePath : faceSrc,
				folderName : $("#exportFolderInput").val(),
				checkWindow : checkFaceWindow
			},
			dataJson: 'json',
			success: function (data) {
				if(data.exportDir){
				var exportedPath = data.exportDir;
				exportedPath=exportedPath.replace(/\//g, "\\");
				$('#displayFacesContainer'+id+' > div:nth-child(2) > div.img-wrap > input:checked').each(function(){
					this.checked=false;
				})
				$('#displayAllFacesContainer'+id+' > div:nth-child(2) > div.img-wrap-allfaces > input:checked').each(function(){
					this.checked=false;
				})
				$('#MetaDataContainer'+id+' input:checked').each(function(){
					this.checked=false;
				})
				var ExportedDirectroy = 'Exported Directory: <span id="exportMessage">'+exportedPath+'</span><a style="cursor:pointer" title="Copy To Clipboard" onclick="copyToClipboard(\'' + id + '\',\''+ checkFaceWindow + '\')"><span class="glyphicon glyphicon-list-alt"></span></a>'
				
				if(checkFaceWindow==1)
					$('#displayFacesContainer'+id+' > div:nth-child(3)> span#alertExportMessage').html(ExportedDirectroy);
				else if(checkFaceWindow==2)
					$('#MetaDataContainer'+id+' > div#deleteExportContainer > span#alertExportMessage').html(ExportedDirectroy);
				else if(checkFaceWindow==3)
					$('#displayAllFacesContainer'+id+' > div:nth-child(3)> span#alertExportMessage').html(ExportedDirectroy);
				
				$('#exportFolderModal').modal('hide');
				$('#exportFolderNameSubmit').unbind('click');
				}
				//failed if path in option.js is not set properly
				else{
					$('#exportFolderModal').modal('hide');
					$('#exportFolderNameSubmit').unbind('click');
					alert('Failed to export selected faces.'+data.failureMessage);
				}
			},	
			error : function(data) {
				$('#exportFolderModal').modal('hide');
				$('#exportFolderNameSubmit').unbind('click');
				alert('Failed to export selected faces.');
			}
		})
	})
}
function copyToClipboard(id, checkFaceWindow) {
	if(checkFaceWindow==1)
		var element = $('#displayFacesContainer'+id+'>div:nth-child(3) > span#alertExportMessage > span#exportMessage');
	else if(checkFaceWindow==2)
		var element = $('#MetaDataContainer'+id+'> div#deleteExportContainer > span#alertExportMessage > span#exportMessage');
	else if(checkFaceWindow==3)
		var element = $('#displayAllFacesContainer'+id+'> div:nth-child(3) > span#alertExportMessage > span#exportMessage');
	var $temp = $("<input>");
	$("body").append($temp);
	$temp.val($(element).text()).select();
	document.execCommand("copy");
	$temp.remove();
	
	//tooltip for getting copied
	if(checkFaceWindow==1){
		$('#displayFacesContainer'+id+' #deleteExportContainer').append('<div class="tooltip-clipboard"><span class="tooltiptext-clipbord">Copied</span></div>');
		$('#displayFacesContainer'+id+' #deleteExportContainer .tooltip-clipboard').fadeIn('slow').delay(3000).fadeOut('slow');
	}
	else if(checkFaceWindow==2){
		$('#MetaDataContainer'+id+' #deleteExportContainer').append('<div class="tooltip-clipboard"><span class="tooltiptext-clipbord">Copied</span></div>');
		$('#MetaDataContainer'+id+' #deleteExportContainer .tooltip-clipboard').fadeIn('slow').delay(3000).fadeOut('slow');
	}
	else if(checkFaceWindow==3){
		$('#displayAllFacesContainer'+id+' #deleteExportContainer').append('<div class="tooltip-clipboard"><span class="tooltiptext-clipbord">Copied</span></div>');
		$('#displayAllFacesContainer'+id+' #deleteExportContainer .tooltip-clipboard').fadeIn('slow').delay(3000).fadeOut('slow');
	}
}

function openFileExplorerSingle(path){
	path = path.substring(0, path.lastIndexOf("/") + 1);
	path = path.replace(/\//g, "\\");
    //console.log('After lastIndexof: '+path);
	$.ajax({
        type: "GET",
        url: "/openFileExplorer",
        data: {
			folderPath : path
 		},
        dataJson: 'json',
		success: function (data) {
			//alert('Success');
        },
        error : function(data) {
 			alert('Failed to open file explorer');
        }
    })
}
function getVideoSize(name, id){
	$.ajax({
        type: "GET",
        url: "/videoSize",
        data: {
			videoName : name
 		},
        dataJson: 'json',
		success: function (data) {
			$('#'+id).find('#fileSize').html(data.Size)
			for(var tempID in fileSizeObject){
				if(tempID==id)
					fileSizeObject[id] = data.Size;
			}
        },
        error : function(data) {
 			alert('Failed to get the video size');
        }
    })
}

function trim(id){
	var data = JSON.parse($("#hdData").val());
	var name = data[id];
	var startTime = $("#startTimeInput").val();
	var endTime = $("#endTimeInput").val();
	$.ajax({
        type: "GET",
        url: "/trimVideo",
        data: {
			StartTrimTime : startTime,
			EndTrimTime : endTime,
			videoName : name
 		},
        dataJson: 'json',
		success: function (data) {
			getVideoSize(name, id);
			$("#pbartext" + id).css('color', '#5cb85c');
			$("#pbartext" + id).text(" Trimmed");
			$('#trimProcess' + id + ' img').remove();
			$('#trimProcess' + id + ' i').removeClass('glyphicon-scissors');
			$('#trimProcess' + id + ' i').addClass('glyphicon-ok green');
			$('#myLoaderModal').modal('hide');
			
			if($('img.trimming-loader').length ==0){
				$("#btnAnalyze").show();
			}
         },
         error : function(data) {
			$("#pbartext" + id).css('color', 'red').text("Error");
			$('#trimProcess' + id + ' img').remove();
			$('#trimProcess' + id + ' i').addClass('glyphicon-warning-sign');
 			alert('Trim UnSuccessful, check the log file for more details');
         }
    })
}

//For Converting Duration Into HH:MM:SS Format
function converSecondsToFormat(totalSeconds){
	var hours = Math.floor(totalSeconds / 3600);
	var totalSeconds = totalSeconds%3600;
	var minutes = Math.floor(totalSeconds / 60);
	var seconds = Math.floor(totalSeconds % 60);
			
	if (hours   < 10) {hours   = "0"+hours;}
    if (minutes < 10) {minutes = "0"+minutes;}
    if (seconds < 10) {seconds = "0"+seconds;}
    return hours+':'+minutes+':'+seconds;
}

function trimNext(id, actualEndTime){
	trimEndTime = $("#endTimeInput").val();
		trimStartTime = $("#startTimeInput").val();
		
		if($("#startTimeInput").val()==""){
			$('#resultForStart').removeClass();
			$('#resultForStart').addClass('notValid');
			$('#resultForStart').html('Input Start Time');
		}
		else if($("#endTimeInput").val()==""){
			$('#resultForEnd').removeClass();
			$('#resultForEnd').addClass('notValid');
			$('#resultForEnd').html('Input End Time');
		}
		else if(!$('#startTimeInput').val().match(/^(?:[0-9]*[0-9][0-9]):[0-5][0-9]:[0-5][0-9]$/)){
			$('#resultForStart').removeClass();
			$('#resultForStart').addClass('notValid');
			$('#resultForStart').html('Start time should be in format HH:MM:SS');
		}
		else if(!$('#endTimeInput').val().match(/^(?:[0-9]*[0-9][0-9]):[0-5][0-9]:[0-5][0-9]$/)){
			$('#resultForEnd').removeClass();
			$('#resultForEnd').addClass('notValid');
			$('#resultForEnd').html('End time should be in format HH:MM:SS');
		}
		else if(trimEndTime>actualEndTime){
			$('#resultForEnd').removeClass();
			$('#resultForEnd').addClass('notValid');
		$('#resultForEnd').html('Enter the end time less than Actual end time of video'+' ['+actualEndTime+']');
		}
		else if(trimStartTime>=trimEndTime){
			if (trimStartTime==trimEndTime){
				$('#resultForEnd').removeClass();
				$('#resultForEnd').addClass('notValid');
				$('#resultForEnd').html('End Time can not be equal to Start time');
				$('#resultForStart').removeClass();
				$('#resultForStart').addClass('notValid');
				$('#resultForStart').html('Start Time can not be equal to End time');
			}
			else{
				$('#resultForEnd').removeClass();
				$('#resultForEnd').addClass('notValid');
				$('#resultForEnd').html('You entered a value which is less than start time');
				$('#resultForStart').removeClass();
				$('#resultForStart').addClass('notValid');
				$('#resultForStart').html('You entered a value which is greater than end time');
			}
		}
		else{
			trim(id);
			$("#pbartext" + id).text("Trimming");
			$("#pbartext" + id).css('color', '#265a88');
			$('#trimProcess' + id + ' i').removeClass('glyphicon-scissors');
			$('#trimProcess' + id).removeClass('btn-primary');
			$('#trimProcess' + id).attr("disabled",true);
			$('#trimProcess' + id).append('<img src="/images/blue-loading.gif" class="trimming-loader">');
			//$("#btnAnalyze").attr("disabled", true);
			$("#btnAnalyze").hide();
			$('#myModal').modal('hide');
			//$('#myLoaderModal').modal('show');
		}
}
function trimCall(id){
	$('#myLoaderModal').modal('show');
	var actualEndTime = "";
	$(startTimeInput).val("00:00:00");
	var data = JSON.parse($("#hdData").val());
	var name = data[id];
	var trimButton = '';
	$.ajax({
        type: "GET",
        url: "/getEndTime",
        data: {
			videoName : name
 		},
        dataJson: 'json',
		success: function (data) {
			//console.log(converSecondsToFormat(data.duration));
			if(data.Status=='Error'){
				$('#myLoaderModal').modal('hide');
				alert('Get end time of video failure.Check the logs for more details');
			}
			else{
				actualEndTime = converSecondsToFormat(data.duration);
				trimButton = '<button style="margin-right:10px" onClick="trimNext(\'' + id + '\',\''+ actualEndTime + '\')" id="submitTrim' + id + '" type="button" class="btn mf-button">Submit <span class="glyphicon glyphicon-ok"></span></button><button type="button" id="closeTrim" class="btn btn-default mf-button" data-dismiss="modal">Close <span class="glyphicon glyphicon-remove"></button>'
				$('.modal-footer.trimFooter').html(trimButton);
				$('.modal-header.trimHeader').html('<button type="button" class="close" data-dismiss="modal">&times;</button><h4 class="modal-title">'+ name +' Trimming Input: </h4>')
				$(endTimeInput).val(actualEndTime);
				$('#myModal').modal('show');
				$('#myLoaderModal').modal('hide');
			}
         },
         error : function(data) {
			if(data.Error){
				alert('get End Time of Video Failure.Check the logs for more details');
			}
         }
    })
}

// It keeps track of which subfile is selcted and not selected
$(document).on('change', '.detectThisChange', function() {
	checkID = $(this).attr("id")
	originalID = checkID.substring(5);
	if($('#'+checkID).attr('checked')) {
		$('#'+checkID).attr( 'checked', false );
		$("#pbartext" + originalID).css('color', '#FF0000');
		$("#pbartext" + originalID).text("Not Selected");
	}
	else{
		$('#'+checkID).attr( 'checked', true );
		$("#pbartext" + originalID).css('color', '#5cb85c');
		$("#pbartext" + originalID).text("Selected");
	}
	$("#selectNoneFiles").show();
});

$( document ).ready(function() {
	$("#containerClose").on("click", function () {
		var data1 = JSON.parse($("#hdData1").val());
			 for(var id in data1){
				remove(id,"hdData1");
		}
	})
	$("#containerCloseX").on("click", function () {
		var data1 = JSON.parse($("#hdData1").val());
			 for(var id in data1){
				remove(id,"hdData1");
		}
	})
    $("#selectNoneFiles").on("click", function () {
		var data = JSON.parse($("#hdDataSubFiles").val());
		for(var id in data){
			$('#check'+id).attr( 'checked', false );
			$("#pbartext" + id).css('color', '#FF0000');
			$("#pbartext" + id).text("Not Selected");
		}
		$("#selectNoneFiles").hide();
	})
});

function deleteSubfiles(){
	$.ajax({
			type: "GET",
			url: "/deleteSubfiles",
			dataJson: 'json',
			success: function (data) {
				//console.log(data);
			},
			error : function(data) {
				alert('Failed to delete subfile');
			}
		});
}

// It appends to the main window of application
function appendToMainWindow(subfileContainerFiles, filesSize){
	var data = JSON.parse($("#hdDataSubFiles").val());
	for (var fileID in data) {
		$('#check' + fileID).attr( 'checked', true );
		$("#pbartext" + fileID).css('color', '#5cb85c');
		$("#pbartext" + fileID).text("Selected");
	}
	$("#extractedSubfilesNext").on("click", function () {
					var htmlAppend = '';
					var i=0;
					var videoDatabases = [];
					$('#myLoaderModal').modal('show');
					var uniqueFiles = JSON.parse($("#hdData").val());
					for (var fileID in data) {
						var duplicateFile = false;
						for(var id in uniqueFiles){
							if(uniqueFiles[id].toLowerCase()==data[fileID].toLowerCase())
								duplicateFile=true;
						}
						if($('#check'+fileID).attr('checked') && !duplicateFile) {
							
							if(data[fileID].match(/[#&=+ ]/g)){
								var message = "The file name "+data[fileID]+" is not supported. Please rename the file, removing nonstandard characters(#, space, +, =, &) and Retry.";
								alert(message);
								remove(fileID,"hdDataSubFiles");
							}
							else{
								htmlAppend += '<li id="' + fileID + '" class="list-group-item">' + data[fileID]+' (<span id="fileSize">'+ filesSize[i] +'</span>)'+'  [' + subfileContainerFiles[i] + ']' + '<div class="row" style="margin-right:1px"><div style="float:right"><div style="float:left; margin-left:1px;" class="priorityTooltip"><input id="priorityTextBox' + fileID + '" type="number" min="0" max="100" step="0" onmouseover="addPriorityTooltip(id)"  onmouseout="removePriorityTooltip(id)" onfocus="addPriorityTooltipFocus(id)" onblur="removePriorityTooltipFocus(id)" class="priorityTextBox"></div><div style="float:left">&nbsp;<button onClick="priorityNext(\'' + fileID + '\')" id="priorityProcess' + fileID + '" type="button" class="btn btn-success" style="height:30px; width:40p; display:none"><i class="glyphicon glyphicon-menu-right"></i></button><button onClick="hpExtractionStop(\'' + fileID + '\')" id="stopProcess' + fileID + '" type="button" class="btn btn-danger" style="height:30px; width:40p; display:none">Stop <i class="glyphicon glyphicon-stop"></i></button><button id="startProcess' + fileID + '" type="button" class="btn btn-info" onClick="hpExtractionStart(\'' + fileID + '\')" style="height:30px; width:40p; display:none">Start <i class="glyphicon glyphicon-play-circle"></i></button><button id="trimProcess' + fileID + '" type="button" onClick="trimCall(\'' + fileID + '\')" class="btn btn-primary" style="height:30px; width:40px;display:none"><i class="glyphicon glyphicon-scissors"></i></button><button onClick="displayFaces(\'' + fileID + '\')" id="next' + fileID + '" type="button" class="btn btn-default" style="height:30px; width:40p;display:none">Faces<i class="glyphicon glyphicon-chevron-right"></i></button><button onClick="metadata(\'' + fileID + '\')" id="displayMetaData' + fileID + '" type="button" class="btn btn-default" style="height:30px;display:none">MetaData<i class="glyphicon glyphicon-chevron-right"></i></button></div>&nbsp;<div class="text-center" id="pbartext' + fileID + '" style="display:inline-block;float:right">Selected</div></div><div class="progress" id="pbar' + fileID + '" style="width:45%;display:inline-block;float:right"><div class="progress-bar progress-bar-striped progress-bar-success" data-transitiongoal="75" style="width: 100%; text-align: center;">100%</div></div></li>';
								
								var databaseName = data[fileID].split(".")[0];
								videoDatabases.push(databaseName);
								
								priorityValues[fileID] = 0;
								append(fileID,data[fileID],"hdData");
								containerSubfiles[fileID] = subfileContainerFiles[i];
								remove(fileID,"hdDataSubFiles");
							}
						}
						else if(($('#check'+fileID).attr('checked') && duplicateFile)){
							var message = "Unable to upload "+data[fileID]+" as this has previously been uploaded. Please rename the video and try again.";
							alert(message);
							remove(fileID,"hdDataSubFiles");
						}
						else{
							remove(fileID,"hdDataSubFiles");
						}
						i++;
					}
					//create database for each selected subfile
					if(videoDatabases.length)
						createDatabase(videoDatabases);
					
					$('#subFileModal').modal('hide');
					$("#filelist").append(htmlAppend);
					
					var data_original = JSON.parse($("#hdData").val());
					for (var fileID in data_original) {
						$('#trimProcess' + fileID).show();
						$("#pbartext" + fileID).css('color', '#5cb85c');
						$("#pbartext" + fileID).text("Uploaded");
					}
					$('#myLoaderModal').modal('hide');
					$(this).unbind('click');
			})
	$("#extractedSubfilesClose").on("click", function(){
		for (var fileID in data) {
			remove(fileID,"hdDataSubFiles");
		}
		$(this).unbind('click');
		$('#extractedSubfilesNext').unbind('click');
	})
	$("#extractedSubfilesCloseX").on("click", function(){
		for (var fileID in data) {
			remove(fileID,"hdDataSubFiles");
		}
		$(this).unbind('click');
		$('#extractedSubfilesNext').unbind('click');
	})
}


//After Extraction Of SubFiles
function AfterFileExtraction(subfileContainerFiles){
	var subfileHtml='';
	$.ajax({
        type: "GET",
        url: "/getSubFiles",
        dataJson: 'json',
		success: function (data) {
			if(data.subFiles.length==0){
				var noFileHtml= '';
				noFileHtml+='<p>No video file in the selected container files</p>'
				document.getElementById('fn2').innerHTML=noFileHtml;
				$('#extractedSubfilesNext').hide();
				$("#selectNoneFiles").hide();
			}
			if(data.subFiles.length>0){
				//console.log(data.subFiles.length);
				  $('#extractedSubfilesNext').show()
				  $("#selectNoneFiles").show();
				  
			for(var a=0; a<data.subFiles.length; a++){
				fileSizeObject[data.subFileID[a]] = data.filesSize[a];
				subfileHtml += '<li id="' + data.subFileID[a] + '" class="list-group-item" style="height:52px"><input id="check' + data.subFileID[a] + '" type="checkbox" class="detectThisChange">' + data.subFiles[a] + '(' + data.filesSize[a] + ')' +' ['+ subfileContainerFiles[a] + ']' + '<div class="text-center" id="pbartext' + data.subFileID[a] + '" style="display:inline-block;float:right">Selected</div></li>';
				append(data.subFileID[a],data.subFiles[a],"hdDataSubFiles"); 
			}
				$('#fn2').html(subfileHtml);
				deleteSubfiles();
				appendToMainWindow(subfileContainerFiles, data.filesSize);
			}
         }
    })
}
//Extraction of subfile from container file
function subfileExtraction(){
	$('#myLoaderModal').modal('show');
	$('#myExtractionModal').modal('hide');
	var data = JSON.parse($("#hdData1").val());
	var containerFiles=0;
	var count1=0;
	
	for (var id in data) {
		containerFiles++;
	}
	
	var subfileContainerFiles = [];
	var lastCount = 0;
	var flagError = false;
	for (var id in data) {
        var name = data[id];
		$.ajax({
         type: "GET",
         url: "/extractSubfile",
		 async: false,
         data: {
 			containerName : name,
		 },
         dataJson: 'json',
         success: function (data) {
			 if(data.Error){
				 flagError=true;
			 }
			 else{
				for(var i = lastCount; i<data.SubfileCount; i++){
					subfileContainerFiles[i] = data.ContainerName
					lastCount = data.SubfileCount;
				}
				count1++;
				if(containerFiles==count1){
					 AfterFileExtraction(subfileContainerFiles);
					 $('#myLoaderModal').modal('hide');
					 $('#subFileModal').modal('show'); 
				}
				var data1 = JSON.parse($("#hdData1").val());
				for(var id in data1){
					remove(id,"hdData1");
				}
			 }
         },
         error : function(data) {
 			alert('Subfile Extraction UnSuccessful.Check the log file for more details');
         }
     })
	}
	if(flagError){
		$('#btnAnalyze').hide();
		$('#myLoaderModal').modal('hide');
		alert('Subfile Extraction UnSuccessful.Check the log file for more details');
	}
}


//for setting priority
function priorityNext(id){
 	var token = JSON.parse($("#hdToken").val());
 	var textValue = $('#priorityTextBox' + id).val();
	
	if(textValue>=0 && textValue<=100){
 	$.ajax({
         type: "GET",
         url: "/givePriority",
         data: {
 			Token :  token[id].currentToken,
 			PriorityValue : textValue
 		},
         dataJson: 'json',
         success: function (data) {
			if(data.Error){
				alert('Failed to give priority, check logs for more details');
			}
			else{
				priorityValues[id] = textValue;
				hpProgress(id,data);
				localStorage.setItem("priorityValuesSession", JSON.stringify(priorityValues));
			}
         },
         error : function(data) {
 			alert('Failed to give priority, check logs for more details');
         }
     })
	}
	else{
		alert("Enter The value between 0-100");
	}
 }

// After extraction is finished
function hpExtractionFinished(id){
	$("#stopPlayUpload" + id).hide();
	$('#nextButtonIcon' + id).show();
}

// send analyze request
function hpRequest(id, name) {
    $.ajax({
        type: "GET",
        url: "/analyze",
		data:{
 			videoName : name,
		},
        dataJson: 'json',
		async: false,
        success: function (data) {
            if (data.currentVideoTotalFrames > -1) {
                append(id,data,"hdToken");
				
				console.log("Current: " + data.currentVideoTotalFrames);
                // add Watcher for checking progress
                var intervalId = setInterval(hpWatcher(id), 5*1000);
                append(id,intervalId,"hdTime");
				var data = $("#hdTime").val();
            }else {
                console.log("video server returns some error");
                var pbartext = $("#pbartext" + id);
                pbartext.css('color', 'red');
                pbartext.text("Error");
                if ("{}" == $("#hdToken").val() || "" == $("#hdToken").val()){
                    initButtonStatus();
                }
            }
        },
        error : function(jqXHR, textStatus, errorThrow) {
            var pbartext = $("#pbartext" + id);
            pbartext.css('color', 'red');
            pbartext.text("Error");
            console.log(jqXHR.responseText);
            console.log(jqXHR.status);
            console.log(jqXHR.readyState);
            console.log(jqXHR.statusText);
            console.log(textStatus);
            console.log(errorThrow);
            if ("{}" == $("#hdToken").val() || "" == $("#hdToken").val()){
                initButtonStatus();
            }
        }
    })
}

function hpAnalyze() {
    $("#start-upload").attr("disabled", true);
	localStorage.setItem("priorityValuesSession", JSON.stringify(priorityValues));
	$(".moxie-shim.moxie-shim-html5").find("input").attr("disabled", true);
	//$("#browse").attr("disabled", true);
	$("#browse").hide();
    var data = JSON.parse($("#hdData").val());
    //$("#btnAnalyze").attr("disabled", true);
	$("#btnAnalyze").hide();
	
	localStorage.setItem('sessionData', JSON.stringify(data));
	localStorage.setItem('sessionFileSizeObject', JSON.stringify(fileSizeObject));
	localStorage.setItem('containerSubfilesObject', JSON.stringify(containerSubfiles));
	//console.log(data);
    for (var id in data) {
		$('#stopPlayUpload' + id).show();
		$('#pbartext' + id).show();
		$('#trimProcess' + id).hide();
        var name = data[id];
        // change label to submitting
        var pbartext = $("#pbartext" + id);
        pbartext.css('color', 'black');
        pbartext.text("Submitting");
        // re-render progress bar
        hpAnalyzeUI(id);
        // send analyze request out
        hpRequest(id, name);
		}
}


function append(id,value,clientId) {
    var data = $("#" + clientId).val();
    if (!data) {
        data = {};
    } else {
        data = JSON.parse(data);
    }
    data[id] = value;
    $("#" + clientId).val(JSON.stringify(data));
    //console.log("appended clientId:" + clientId + $("#" + clientId).val());
}

function remove(id,clientId) {
    var data = $("#" + clientId).val();
    if (data) {
        data = JSON.parse(data);
		//console.log(data);
        delete data[id];
        $("#" + clientId).val(JSON.stringify(data));
        //console.log("removed clientId:" + clientId + $("#" + clientId).val());
    }
}
function verifyUploadedConfig(){
	$.ajax({
		type: "GET",
        url: "/verifyUploadedConfig",
        dataJson: 'json',
		async: false,
        success: function (data) {
			if(data.Error){
					var ErrorMessage = data.Error+'.For more details check the log file('+ data.logFilePath +').';
					var html='<div class="alert alert-danger alert-dismissable fade in"><span></sapn><a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><strong><span class="glyphicon glyphicon-hand-right"></span>  Some error with the config!</strong><br>'+ErrorMessage+'</div>';
					$("#configStatus").html('<span class="glyphicon glyphicon-alert"></span> Unable to update');
					document.getElementById("configStatus").style.color = "#a94442";
					$('div#validateConfig').show().append(html);
			}
			else{
				if(data.successMessage){
					var html='<div class="alert alert-success alert-dismissable fade in"><span></sapn><a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><strong><span class="glyphicon glyphicon-ok"></span>  Configuration Changed Successfully!</strong></div>';
					$('#validateConfig').show().html(html);
				}
				if(data.missingParamMessage){
					var missingParamMessage=data.missingParamMessage;
					if(missingParamMessage.length>0){
						missingParamMessage=missingParamMessage.split('.').join('.<br>');
						var html='<div class="alert alert-danger alert-dismissable fade in"><span></sapn><a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><strong><span class="glyphicon glyphicon-hand-right"></span>  Some of config params are not set properly!</strong><br>'+missingParamMessage+'</div>';
						$("#configStatus").html('<span class="glyphicon glyphicon-alert"></span> Unable to update');
						document.getElementById("configStatus").style.color = "#a94442";
						$('#validateConfig').show().html(html);
					}
				}
				if(data.defaultValuesMessage){
						var defaultValuesMessage = data.defaultValuesMessage;
						defaultValuesMessage=defaultValuesMessage.split('.').join('.<br>');
						var html='<div class="alert alert-info alert-dismissable fade in"><span></sapn><a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><strong><span class="glyphicon glyphicon-info-sign"></span>  Some of config params are set default!</strong><br>'+defaultValuesMessage+'</div>';
						$('div#validateConfig').append(html);
				}
			}
			 
			 $('#myLoaderModal').modal('hide');
			 $("#changeConfiguration").hide();
		},
		error: function(data){
			$('#myLoaderModal').modal('hide');
		}
	});
}
function changeConfiguration(){
	//$("#changeConfiguration").attr("disabled", true);
	$("#changeConfiguration").hide();
	$("#configStatus").html('');
	var uploader_config = new plupload.Uploader({
        browse_button: 'browse_config',
        url: '/uploadConfiguration',
        multipart: true,
		multi_selection: false,
        filters: {
            mime_types : [
                { title : "Configuration files", extensions : "js" }
            ]
        }
    });
	uploader_config.init();
	uploader_config.bind('FilesAdded', function (up, files) {
		$("#validateConfig").hide();
		//$("#changeConfiguration").attr("disabled", false);
		$("#changeConfiguration").show();
		var configHtml = '';
		plupload.each(files, function (file) {
			configHtml += '<li id="' + file.id + '" class="list-group-item">' + file.name + ' ( ' + plupload.formatSize(file.size) + ')<div class="row" style="margin-right:1px"><div style="float:right"><span id="configStatusIcon" style="float:left"></span>&nbsp;<div id="configStatus" style="float:right">Selected</div></div></div></li>';
			document.getElementById('fn2').innerHTML=configHtml;
        })
		//$('#myLoaderModal').modal('show');
        uploader_config.start();
	});
	uploader_config.bind('UploadComplete', function () {
		//$("#changeConfiguration").attr("disabled", true);
		//$("#changeConfiguration").hide();
		//$("#configStatus").html('Configuration Changed Successfully');
		//document.getElementById("configStatus").style.color = "#049e11";
		//$("#configStatus").append("<span class="glyphicon glyphicon-ok"></span>");
		//$("#configStatusIcon").addClass("glyphicon glyphicon-ok green");
		//$('#myLoaderModal').modal('hide');
		//verifyUploadedConfig();
    });
	
	$("#changeConfiguration").on("click", function () {
		//$('#myLoaderModal').modal('show');
        //uploader_config.start();
		$('#myLoaderModal').modal('show');
		$("#configStatus").html('');
		verifyUploadedConfig();
	});
}
function createDatabase(videoDatabases){
	//console.log(videoDatabases);

	$.ajax({
		type: 'GET',
		url: '/createVideoDatabases',
		data: {listDatabases:videoDatabases},
		dataJson: 'json',
		success: function (data) {
		},
		error : function(data) {
			alert('Failed to create Databases');
		}
	});
}

function removeAllDatabases(){
	$.ajax({
		type: 'GET',
		url: '/removeAllDatabases',
		dataJson: 'json',
		success: function (data) {
		},
		error : function(data) {
			alert('Failed to remove Databases');
		}
	});
}

var html = '';
var fileSizeObject = {};
var containerSubfiles = {};
function fileUploader(){
	localStorage.setItem('sessionData', JSON.stringify(''));
	localStorage.setItem('sessionTokenObjTemp', JSON.stringify(''));
	localStorage.setItem('fileSizeObject', JSON.stringify(''));
	localStorage.setItem('containerSubfiles', JSON.stringify(''));
	
	$("#hdData").val("{}");
	$('#configurationSettings').show();
	
    var uploader = new plupload.Uploader({
        browse_button: 'browse',
        url: '/upload',
        multipart: true
    });
    uploader.init();
	
    uploader.bind('FilesAdded', function (up, files) {
		var html1='';
		html = '';
		var videoDatabases = [];
		var videoFile = false;
        //$("#hdData").val("{}");
		var startupload=false;
        plupload.each(files, function (file) {
			var fileextension=file.name;
			fileextension=fileextension.split(".");
			fileextension=fileextension[1];
			
		$.ajax({
			type: "GET",
			url: "/checkFileExtension",
			async : false,
			data: {
				fileExtension : fileextension
			},
			dataJson: 'json',
			success: function (data) {
				var uniqueFiles = JSON.parse($("#hdData").val());
				var duplicateFile = false;
				for(var id in uniqueFiles){
					if((uniqueFiles[id]).toLowerCase()==(file.name).toLowerCase())
					duplicateFile=true;
				}
				
				if(data.includes("1")){
					if(file.name.match(/[#&=+ ]/g)){
						var message = "The file name "+file.name+" is not supported. Please rename the file, removing nonstandard characters(#, space, +, =, &) and Retry.";
						alert(message);
						uploader.removeFile(file);
					}
					else{
					//alert('Supported Format');
					append(file.id,file.name,"hdData");
					
					priorityValues[file.id] = 0;
					
					//creating database for each video
					//createDatabase(JSON.parse($("#hdData").val()));
					var databaseName = file.name;
					databaseName = databaseName.split(".")[0];
					videoDatabases.push(databaseName);
					videoFile=true;
					
					html += '<li id="' + file.id + '" class="list-group-item" style="min-height:42px;overflow: hidden;">' + file.name + ' ( <span id="fileSize">' + plupload.formatSize(file.size) + '</span>)<div class="row" style="margin-right:1px"><div style="float:right"><div style="float:left; margin-left:1px;" class="priorityTooltip"><input id="priorityTextBox' + file.id + '" type="number" min="0" max="100" step="0" onmouseover="addPriorityTooltip(id)"  onmouseout="removePriorityTooltip(id)" onfocus="addPriorityTooltipFocus(id)" onblur="removePriorityTooltipFocus(id)" class="priorityTextBox"></div><div style="float:left">&nbsp;<button onClick="priorityNext(\'' + file.id + '\')" id="priorityProcess' + file.id + '" type="button" class="btn btn-success" style="height:30px; width:40p; display:none"><i class="glyphicon glyphicon-menu-right"></i></button><button onClick="hpExtractionStop(\'' + file.id + '\')" id="stopProcess' + file.id + '" type="button" class="btn btn-danger" style="height:30px; width:40p; display:none">Stop <i class="glyphicon glyphicon-stop"></i></button><button id="startProcess' + file.id + '" type="button" class="btn btn-info" onClick="hpExtractionStart(\'' + file.id + '\')" style="height:30px; width:40p; display:none">Start <i class="glyphicon glyphicon-play-circle"></i></button><button id="trimProcess' + file.id + '" type="button" onClick="trimCall(\'' + file.id + '\')" class="btn btn-primary" style="height:30px; width:40px;display:none"><i class="glyphicon glyphicon-scissors"></i></button><button onClick="displayFaces(\'' + file.id + '\')" id="next' + file.id + '" type="button" class="btn btn-default" style="height:30px; width:40p;display:none">Faces<i class="glyphicon glyphicon-chevron-right"></i></button></div>&nbsp;<div class="text-center" id="pbartext' + file.id + '" style="display:inline-block;float:right">Selected</div></div><div class="progress" id="pbar' + file.id + '" style="width:45%;display:inline-block;float:right"><div class="progress-bar progress-bar-striped active" data-transitiongoal="75"></div></div></div></li>';
					$('#configurationSettings').hide();
					fileSizeObject[file.id] = plupload.formatSize(file.size)
					uploader.start();
					}
				}
				else if(duplicateFile){
					var message = "Unable to upload "+file.name+" as this has previously been uploaded. Please rename the video and try again.";
					alert(message);
					uploader.removeFile(file);		
				}
				else if(data.includes("2")){
					//alert('container format');
					append(file.id,file.name,"hdData1");
					//html1 += '<li id="' + file.id + '" class="list-group-item">' + file.name + ' ( ' + plupload.formatSize(file.size) + ')<div class="text-center" id="pbartext' + file.id + '" style="display:inline-block;float:right">Selected</div></li>';
					html1 += '<li id="' + file.id + '" class="list-group-item" style="min-height:42px;overflow: hidden;">' + file.name + ' ( ' + plupload.formatSize(file.size) + ') <div class="text-center" id="pbartext' + file.id + '" style="width:10%;display:inline-block;float:right;margin-left:5px">Uploading</div><div class="progress" id="pbar' + file.id + '" style="height:17px;width:40%;display:inline-block;float:right"><div class="progress-bar progress-bar-striped active" data-transitiongoal="75"/></div></div></li>';
					document.getElementById('fn1').innerHTML=html1;
					$('#myExtractionModal').modal('show');
					$('#configurationSettings').hide();
					uploader.start();
				}
				else{
					alert(file.name +' not supported for processing');
					uploader.removeFile(file);
				}
			},
			error : function(data) {
				alert('Error While Detecting the format');
			}
		  })
        })		
		
		//create database for each video
		if(videoFile){
			createDatabase(videoDatabases);
		}
		
        $("#filelist").append(html);
        $("#start-upload").attr("disabled", false);
        //$("#btnAnalyze").attr("disabled", true);
		//$("#btnAnalyze").hide();
    });
    
	uploader.bind('UploadProgress', function (up, file) {
        var pbar = $("#pbar" + file.id);
        if (100 == file.percent) {
			/*
            pbar.removeClass('progress');
            pbar.css('color', 'green');
            pbar.css('text-align', 'right');
            pbar.text("");*/
			var bar = pbar.find('.progress-bar');
			bar.removeClass('progress-bar-primary');
			bar.removeClass('active');
            bar.addClass('progress-bar-success');
			bar.css('width',"100%");
			bar.css('text-align', 'center');
			bar.text("100%");
            var pbartext = $("#pbartext" + file.id);
            pbartext.css('color', '#5cb85c');
            pbartext.text("Uploaded");
			$("#trimProcess" + file.id).show();
			$("#trimProcess" + file.id).show();
        } else {
            var bar = pbar.find('.progress-bar');
            bar.addClass('progress-bar-primary');
            bar.css('width', file.percent + "%");
            bar.css('text-align', 'center');
            bar.text(file.percent + "%");
        }
    })
    uploader.bind('UploadComplete', function () {
		if($('img.trimming-loader').length ==0){
        //$("#btnAnalyze").attr("disabled", false);
		$("#btnAnalyze").show();
		}
		$("#containerNext").attr("disabled", false);
    })
	/*
    $("#start-upload").on("click", function () {
        $("#start-upload").attr("disabled", true);
        uploader.start();
    });*/
	
}

function addPriorityTooltip(priorityBoxId){
	$('input#'+priorityBoxId).before('<span id="alertRange" class="tooltiptext">0 Lowest, 100 highest priority.</span>');
	$("#alertRange").fadeIn('slow').delay(5000).fadeOut('slow');
}

function removePriorityTooltip(priorityBoxId){
	$('#alertRange').remove(); 
}

function addPriorityTooltipFocus(priorityBoxId){
	$('input#'+priorityBoxId).before('<span id="alertRangeFocus" class="tooltiptext">0 Lowest, 100 highest priority.</span>');
	$("#alertRangeFocus").fadeIn('slow').delay(5000).fadeOut('slow');
}

function removePriorityTooltipFocus(priorityBoxId){
	$('#alertRangeFocus').remove(); 
}

function validateConfigParams(){
	$.ajax({
         type: "GET",
         url: "/checkConfigParams",
         data: {
 		 },
         dataJson: 'json',
         success: function (data) {
			 var Message=data.missingParamMessage;
			 if(Message.length>0){
				$("#browse").hide();
				Message=Message.split('.').join('.<br>');
				var html='<div class="alert alert-danger alert-dismissable fade in"><span></sapn><a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><strong><span class="glyphicon glyphicon-hand-right"></span>  Some of config params are not set properly!</strong><br>'+Message+'</div>';
				$('#MissssingParamContainer').show().html(html);
			 }
         },
         error : function(data) {
			 alert('Failed to validate config params')
         }
     })
}

function initiate() {
	validateConfigParams();
	var sessionObj = JSON.parse(localStorage.getItem('sessionData'));
	//console.log(sessionObj);
	if(sessionObj!=''){
	var answer = confirm("Continue from previous Session data?")
	if (answer) {
			$('#myLoaderModal').modal('show');
			var sessionObj = JSON.parse(localStorage.getItem('sessionData'));
			var fileSizeObject = JSON.parse(localStorage.getItem('sessionFileSizeObject'));
			
			for(var id in sessionObj){
				html += '<li id="' + id + '" class="list-group-item" style="min-height:42px;overflow: hidden;">' + sessionObj[id] + ' ( <span id="fileSize">' + fileSizeObject[id] + '</span>)<span id="ContainerName"></span><div class="row" style="margin-right:1px"><div style="float:right"><div style="float:left; margin-left:1px;" class="priorityTooltip"><input id="priorityTextBox' + id + '" type="number" min="0" max="100" step="0" onmouseover="addPriorityTooltip(id)"  onmouseout="removePriorityTooltip(id)" onfocus="addPriorityTooltipFocus(id)" onblur="removePriorityTooltipFocus(id)" class="priorityTextBox"></div><div style="float:left">&nbsp;<button onClick="priorityNext(\'' + id + '\')" id="priorityProcess' + id + '" type="button" class="btn btn-success" style="height:30px; width:40p; display:none"><i class="glyphicon glyphicon-menu-right"></i></button><button onClick="hpExtractionStop(\'' + id + '\')" id="stopProcess' + id + '" type="button" class="btn btn-danger" style="height:30px; width:40p; display:none">Stop <i class="glyphicon glyphicon-stop"></i></button><button id="startProcess' + id + '" type="button" class="btn btn-info" onClick="hpExtractionStart(\'' + id + '\')" style="height:30px; width:40p; display:none">Start <i class="glyphicon glyphicon-play-circle"></i></button><button id="trimProcess' + id + '" type="button" onClick="trimCall(\'' + id + '\')" class="btn btn-primary" style="height:30px; width:40px;display:none"><i class="glyphicon glyphicon-scissors"></i></button><button onClick="displayFaces(\'' + id + '\')" id="next' + id + '" type="button" class="btn btn-default" style="height:30px; width:40p;display:none">Faces<i class="glyphicon glyphicon-chevron-right"></i></button></div>&nbsp;<div class="text-center" id="pbartext' + id + '" style="display:inline-block;float:right">Selected</div></div><div class="progress" id="pbar' + id + '" style="width:45%;display:inline-block;float:right"><div class="progress-bar progress-bar-striped" data-transitiongoal="75"></div></div></div></li>';
				append(id,sessionObj[id],"hdData");
			}
			$("#filelist").append(html);
		
			var sessionTokenObj = JSON.parse(localStorage.getItem('sessionTokenObjTemp'));
			for(var id in sessionTokenObj){
				append(id,sessionTokenObj[id],"hdToken");
			}
			
			var sessionContainerSubfiles = JSON.parse(localStorage.getItem('containerSubfilesObject'));
			for(var id in sessionContainerSubfiles){
				$('#'+id).find('#ContainerName').html(' ['+sessionContainerSubfiles[id]+']')
			}
			
			for(var id in sessionObj){
				var intervalId = setInterval(hpWatcher(id), 5*1000);
				append(id,intervalId,"hdTime");
			}
			
			$("#start-upload").attr("disabled", true);
			$(".moxie-shim.moxie-shim-html5").find("input").attr("disabled", true);
			//$("#browse").attr("disabled", true);
			$("#browse").hide();
		}
		else {
			removeAllDatabases()
			fileUploader();
		}
	}
	else{
		removeAllDatabases();
		fileUploader();
	}
}