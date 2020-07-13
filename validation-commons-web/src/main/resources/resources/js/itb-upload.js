var _config = {},
    _state = {};
_state.listeners = {};
_state.listenerEvents = {};
_state.listenerEvents['ADDED_EXTERNAL_ARTIFACT_INPUT'] = true;
_state.listenerEvents['REMOVED_EXTERNAL_ARTIFACT_INPUT'] = true;
_state.listenerEvents['RESET_EXTERNAL_ARTIFACT_INPUTS'] = true;
_state.listenerEvents['INPUT_CONTENT_TYPE_CHANGED'] = true;

$(document).ready(function() {
	checkForSubmit();
	resetExternalArtifacts();
	if (document.getElementById('text-editor') !== null){
		CodeMirror.fromTextArea(document.getElementById('text-editor'), {
	        mode: _config.codeTypeObj,
	        lineNumbers: true
	    }).on('change', function(){
	    	contentSyntaxChanged();
	    });
	}
    $("body").tooltip({ selector: '[data-toggle=tooltip]' });
});

function configure(config) {
    if (config) {
        if (config.externalArtifactFileLabel) {
            _config.externalArtifactFileLabel = config.externalArtifactFileLabel;
        }
        if (config.externalArtifactURILabel) {
            _config.externalArtifactURILabel = config.externalArtifactURILabel;
        }
        if (config.externalArtifactSettings) {
            _config.externalArtifactSettings = config.externalArtifactSettings;
        }
        if (config.isMinimalUI) {
            _config.isMinimalUI = config.isMinimalUI;
        }
        if (config.codeTypeObj) {
            _config.codeTypeObj = config.codeTypeObj;
        }
        if (config.artifactTypes) {
            _config.artifactTypes = config.artifactTypes;
        }
        if (config.externalArtifacts) {
            _config.externalArtifacts = config.externalArtifacts;
        }
    }
}

function addListener(eventType, fn) {
    if (!eventType) {
        console.error('Event type must be provided for listener');
    } else if (!_state.listenerEvents[eventType]) {
        console.error('Invalid event type for listener ['+eventType+']');
    } else {
        if (!_state.listeners[eventType]) {
            _state.listeners[eventType] = [];
        }
        _state.listeners[eventType].push(fn);
    }
}

function notifyListeners(eventType, eventData) {
    if (_state.listeners[eventType]) {
        for (var i=0; i < _state.listeners[eventType].length; i++) {
            _state.listeners[eventType][i](eventType, eventData);
        }
    }
}

function validationTypeChanged() {
	cleanExternalArtifacts();
	checkForSubmit();
	resetExternalArtifacts();
}

function cleanExternalArtifacts() {
    var i, artifactType, j;
    for (i=0; i < _config.artifactTypes.length; i++) {
        artifactType = _config.artifactTypes[i],
            elements = $(".externalDiv_"+artifactType);
        for (j=0; j<elements.length; j++){
            removeElement(artifactType, elements[j].getAttribute('id'));
        }
    }
}

function resetExternalArtifacts() {
    var includeExternalArtefacts, i, artifactType;
	$(".includeExternalArtefacts").addClass('hidden');	
	$("#externalArtefactsCheck").prop('checked', false);
	includeExternalArtefacts = getExternalArtifactSupport();
	if (includeExternalArtefacts == "optional") {
        $(".includeExternalArtefacts").removeClass('hidden');
	} else {
		$(".includeExternalArtefacts").addClass('hidden');
	}
    for (i=0; i < _config.artifactTypes.length; i++) {
        artifactType = _config.artifactTypes[i],
            toggleExt = getExternalArtifactSupport(artifactType);
        if (toggleExt == "required") {
            addElement(artifactType, placeholderTextForExternalArtifactFile(artifactType), false);
            $('#rmvButton-external_'+artifactType+'-1').addClass('hidden');
            $('#fileToValidate-class-external_'+artifactType+'-1').removeClass('col-sm-11');
            $('#fileToValidate-class-external_'+artifactType+'-1').addClass('col-sm-12');
            $('#uriToValidate-external_'+artifactType+'-1').removeClass('col-sm-11');
            $('#uriToValidate-external_'+artifactType+'-1').addClass('col-sm-12');
        }
        if (toggleExt == "none" || includeExternalArtefacts == "optional") {
            $('.externalClass_'+artifactType).addClass('hidden');
        } else {
            $('.externalClass_'+artifactType).removeClass('hidden');
        }
    }
	notifyListeners('RESET_EXTERNAL_ARTIFACT_INPUTS', {});
}

function maxExternalArtifacts(artifactType) {
    if (_config.externalArtifactSettings[artifactType] && _config.externalArtifactSettings[artifactType].maxCount) {
        return _config.externalArtifactSettings[artifactType].maxCount;
    }
    return -1;
}

function placeholderTextForExternalArtifactFile(artifactType) {
    if (_config.externalArtifactSettings[artifactType] && _config.externalArtifactSettings[artifactType].filePlaceholder) {
        return _config.externalArtifactSettings[artifactType].filePlaceholder;
    }
    return '';
}

function addExternal(artifactType) {
    var toggleExt, indexInt;
	toggleExt = getExternalArtifactSupport(artifactType),
        elements = $(".externalDiv_" + artifactType);
	if (toggleExt == "required" && elements.length == 1) {
		indexInt = getLastExternalIdNumber(".externalDiv_"+artifactType);
		$('#rmvButton-external_'+artifactType+'-'+indexInt).removeClass('hidden');
		$('#fileToValidate-class-external_'+artifactType+'-'+indexInt).removeClass('col-sm-12');
		$('#fileToValidate-class-external_'+artifactType+'-'+indexInt).addClass('col-sm-11');
		$('#uriToValidate-external_'+artifactType+'-'+indexInt).removeClass('col-sm-12');
		$('#uriToValidate-external_'+artifactType+'-'+indexInt).addClass('col-sm-11');
	}
	addElement(artifactType, placeholderTextForExternalArtifactFile(artifactType), true);
}

function removeElement(artifactType, elementId) {
	$('#'+elementId).remove();
	var toggleExt = getExternalArtifactSupport(artifactType),
        elements = $(".externalDiv_"+artifactType),
        maxInputs, indexInt;
	if (toggleExt == "required" && elements.length == 1){
		indexInt = getLastExternalIdNumber(".externalDiv_"+artifactType);
		$('#rmvButton-external_'+artifactType+'-'+indexInt).addClass('hidden');
		$('#fileToValidate-class-external_'+artifactType+'-'+indexInt).removeClass('col-sm-11');
		$('#fileToValidate-class-external_'+artifactType+'-'+indexInt).addClass('col-sm-12');
		$('#uriToValidate-external_'+artifactType+'-'+indexInt).removeClass('col-sm-11');
		$('#uriToValidate-external_'+artifactType+'-'+indexInt).addClass('col-sm-12');
	}
    maxInputs = maxExternalArtifacts(artifactType);
    if (maxInputs >= 0) {
        if (elements.length < maxInputs) {
            $('#externalAddButton_'+artifactType).removeClass('hidden');
        }
    }
    fixMargins(artifactType);
	notifyListeners('REMOVED_EXTERNAL_ARTIFACT_INPUT', {'artifactType': artifactType});
    checkForSubmit();
}

function getLastExternalIdNumber(type){
    var elements = $(type),
	    index = elements.attr('id').indexOf("-")+1,
	    indexInt = parseInt(elements.attr('id').substring(index));
	return indexInt;
}

function triggerFileUploadExternal(elementId) {
    $("#"+elementId).click();
}
function fileInputChangedExternal(type){
	if($('#contentType-'+type).val()=="fileType" && $("#inputFile-"+type+"")[0].files[0]!=null){
		$("#inputFileName-"+type+"").val($("#inputFile-"+type+"")[0].files[0].name);
	}
	checkForSubmit();
}
function fileInputChanged() {
	if($('#contentType').val()=="fileType" && $('#inputFile')[0].files[0]!=null){
		$('#inputFileName').val($('#inputFile')[0].files[0].name);
	}
	checkForSubmit();
}
function contentTypeChangedExternal(elementId){
	var type = $('#contentType-'+elementId).val();
	if (type == "uriType"){
		$("#uriToValidate-"+elementId).removeClass('hidden');
		$("#fileToValidate-"+elementId).addClass('hidden');
	} else if (type == "fileType"){
		$("#fileToValidate-"+elementId).removeClass('hidden');
		$("#uriToValidate-"+elementId).addClass('hidden');
	}
	fileInputChanged(elementId);
}

function addElement(artifactType, placeholderText, focus) {
    var elements = $(".externalDiv_"+artifactType),
        indexLast = 0,
        i, index, indexInt2, elementId, maxInputs, eventInfo;
	for (i=0; i<elements.length; i++){
		index = elements[i].getAttribute('id').indexOf("-")+1;
        indexInt2 = parseInt(elements[i].getAttribute('id').substring(index));
		if (indexInt2>indexLast) {
			indexLast = indexInt2;
		}
	}
	elementId = "external_"+artifactType+"-"+(indexLast+1);
    $("<div class='row externalDiv_"+artifactType+"' id='"+elementId+"'>" +
    	"<div class='col-sm-2'>"+
			"<select class='form-control' id='contentType-"+elementId+"' name='contentType-external_"+artifactType+"' onchange='contentTypeChangedExternal(\""+elementId+"\")'>"+
				"<option value='fileType' selected='true'>"+_config.externalArtifactFileLabel+"</option>"+
				"<option value='uriType'>"+_config.externalArtifactURILabel+"</option>"+
		    "</select>"+
		"</div>"+
		"<div class='col-sm-10'>" +
		    "<div class='row'>" +
                "<div id='fileToValidate-class-"+elementId+"' class='col-sm-11'>" +
                    "<div class='input-group' id='fileToValidate-"+elementId+"'>" +
                        "<div class='input-group-btn'>" +
                            "<button class='btn btn-default' type='button' onclick='triggerFileUploadExternal(\"inputFile-"+elementId+"\")'><i class='far fa-folder-open'></i></button>" +
                        "</div>" +
                        "<input type='text' id='inputFileName-"+elementId+"' placeholder='"+placeholderText+"' class='form-control clickable' onclick='triggerFileUploadExternal(\"inputFile-"+elementId+"\")' readonly='readonly'/>" +
                    "</div>" +
                "</div>" +
                "<div class='col-sm-11 hidden' id='uriToValidate-"+elementId+"'>"+
                    "<input type='url' class='form-control' id='uri-"+elementId+"' name='uri-external_"+artifactType+"' onchange='fileInputChangedExternal(\""+elementId+"\")'>"+
                "</div>"+
                "<input type='file' class='inputFile' id='inputFile-"+elementId+"' name='inputFile-external_"+artifactType+"' onchange='fileInputChangedExternal(\""+elementId+"\")'/>" +
                "<div class='col-sm-1'>" +
                    "<button class='btn btn-default' id='rmvButton-"+elementId+"' type='button' onclick='removeElement(\""+artifactType+"\",\""+elementId+"\")'><i class='far fa-trash-alt'></i></button>" +
                "</div>" +
    		"</div>"+
		"</div>"+
    "</div>").insertBefore("#externalAddButton_"+artifactType);
    if (focus) {
        $("#"+elementId+" input").focus();
    }
    maxInputs = maxExternalArtifacts(artifactType);
    if (maxInputs >= 0) {
        if ($('.externalDiv_'+artifactType).length >= maxInputs) {
            $('#externalAddButton_'+artifactType).addClass('hidden');
        }
    }
    fixMargins(artifactType);
    eventInfo = {
        'elementId': elementId,
        'elementIndex': (indexLast+1),
        'artifactType': artifactType
    }
	notifyListeners('ADDED_EXTERNAL_ARTIFACT_INPUT', eventInfo);
}

function fixMargins(artifactType) {
    var inputRows = $('.externalDiv_'+artifactType),
        addButton = $('#externalAddButton_'+artifactType);
    inputRows.removeClass('external-input-margin');
    addButton.removeClass('external-input-margin');
    inputRows.each(function(index) {
        if (index > 0) {
            $(this).addClass('external-input-margin');
        }
    });
    if (inputRows.length > 0) {
        addButton.addClass('external-input-margin');
    }
}

function checkForSubmit() {
	var type = $('#contentType').val(),
	    inputType = $('#validationType'),
	    submitDisabled = true,
	    inputFile, uriInput, stringType, i;
	$('#inputFileSubmit').prop('disabled', true);	
	if (type == "fileType") {
		inputFile = $("#inputFileName");
		submitDisabled = (inputFile.val() && (!inputType.length || inputType.val()))?false:true
	} else if (type == "uriType") {
		uriInput = $("#uri");
		submitDisabled = (uriInput.val() && (!inputType.length || inputType.val()))?false:true
	} else if (type == "stringType") {
		stringType = getCodeMirrorNative('#text-editor').getDoc();
		submitDisabled = (stringType.getValue() && (!inputType.length || inputType.val()))?false:true
	}
	if (!submitDisabled) {
        for (i=0; i < _config.artifactTypes.length; i++) {
            if (getExternalArtifactSupport(_config.artifactTypes[i]) == 'required') {
                submitDisabled = !externalElementHasValue(document.getElementsByName("contentType-external_"+_config.artifactTypes[i]));
                if (submitDisabled) {
                    break;
                }
            }
        }

	}

    if (_config.isMinimalUI) {
        $('#inputFileSubmitMinimal').prop('disabled', submitDisabled);
    } else {
        $('#inputFileSubmit').prop('disabled', submitDisabled);
    }

}

function externalElementHasValue(elementExt) {
	if (elementExt.length > 0) {
	    var i, type, id;
	    for (i=0; i < elementExt.length; i++) {
            type = elementExt[i].options[elementExt[i].selectedIndex].value;
            id = elementExt[i].id.substring("contentType-".length, elementExt[i].id.length);
            if (type == "fileType" && $("#inputFileName-"+id).val() || type == "uriType" && $("#uri-"+id).val()) {
                return true;
            }
	    }
	}
	return false;
}

function triggerFileUpload() {
	$('#inputFile').click();
}
function uploadFile() {
	waitingDialog.show('Validating input', {dialogSize: 'm'}, _config.isMinimalUI?'busy-modal-minimal':'busy-modal');
	return true;
}

function contentTypeChanged() {
	var type = $('#contentType').val();
	$('#inputFileSubmit').prop('disabled', true);
	if (type == "uriType"){
		$("#uriToValidate").removeClass('hidden');
		$("#fileToValidate").addClass('hidden');
		$("#stringToValidate").addClass('hidden');
	} else if (type == "fileType") {
		$("#fileToValidate").removeClass('hidden');
		$("#uriToValidate").addClass('hidden');
		$("#stringToValidate").addClass('hidden');
	} else if (type == "stringType") {
		$("#stringToValidate").removeClass('hidden');
		$("#uriToValidate").addClass('hidden');
		$("#fileToValidate").addClass('hidden');
		setTimeout(function() {
            var codeMirror = getCodeMirrorNative('#text-editor')
            codeMirror.refresh();
		}, 0);
	}
    notifyListeners('INPUT_CONTENT_TYPE_CHANGED', {});
}
function getCodeMirrorNative(target) {
    var _target = target;
    if (typeof _target === 'string') {
        _target = document.querySelector(_target);
    }
    if (_target === null || !_target.tagName === undefined) {
        throw new Error('Element does not reference a CodeMirror instance.');
    }
    
    if (_target.className.indexOf('CodeMirror') > -1) {
        return _target.CodeMirror;
    }

    if (_target.tagName === 'TEXTAREA') {
        return _target.nextSibling.CodeMirror;
    }
    
    return null;
}
function contentSyntaxChanged() {
	checkForSubmit();
}

function toggleExternalArtefacts() {
    var checked = $("#externalArtefactsCheck").is(":checked"),
        i, toggleExt, ext;
    if (checked) {
        for (i=0; i < _config.artifactTypes.length; i++) {
            toggleExt = getExternalArtifactSupport(_config.artifactTypes[i]);
            ext = getExternalArtifactSupport(_config.artifactTypes[i]);
            if (ext == "none") {
                $(".externalClass_"+_config.artifactTypes[i]).addClass("hidden");
            } else {
                $(".externalClass_"+_config.artifactTypes[i]).removeClass("hidden");
            }
        }
    } else {
        for (i=0; i < _config.artifactTypes.length; i++) {
            $(".externalClass_"+_config.artifactTypes[i]).addClass("hidden");
        }
        cleanExternalArtifacts();
    }
}

function getExternalArtifactSupport(artifactType) {
	var supportType = "none",
	    validationTypeElement = $('#validationType'),
	    type, validationTypes, currentArtifactType;
    if (validationTypeElement.length) {
        type = $('#validationType').val();
    } else {
        if (_config.externalArtifacts) {
            validationTypes = Object.keys(_config.externalArtifacts);
            if (validationTypes && validationTypes.length && validationTypes.length > 0) {
                type = validationTypes[0];
            }
        }
    }
	if (type) {
        if (artifactType) {
            supportType = _config.externalArtifacts[type][artifactType];
        } else {
            for (currentArtifactType in _config.externalArtifacts[type]) {
                if (_config.externalArtifacts[type][currentArtifactType] == "required") {
                    supportType = "required";
                    break;
                } else if (supportType == "none" && _config.externalArtifacts[type][currentArtifactType] == "optional") {
                    supportType = "optional";
                }
            }
        }
	}
    return supportType;
}

function getReportData(inputID) {
    _state.reportLoad = jQuery.Deferred();
    _state.resultLoadXML = jQuery.Deferred();
    _state.resultLoadPDF = jQuery.Deferred();
    $(document).ready(function() {
        getReport(inputID);
        getResultReport(inputID);
    });
}
function getReport(inputID) {
	$.get("input/"+inputID, function(data) {
		_state.itbReportData = data;
		$.ajax({
			url: "input/"+inputID,
			type: 'DELETE'
		});
		_state.reportLoad.resolve();
		$('#viewInputButton').prop('disabled', false);
	});
}
function getResultReport(inputID) {
	$.ajax({
		url: "report/"+inputID+"/xml",
		type: 'GET',
		success: function(data) {
			_state.itbResultReportXML = new Blob([data], { type: 'application/xml' });
            $('#downloadReportButtonXML').prop('disabled', false);
			_state.resultLoadXML.resolve();
		}
	});

    var ajax = new XMLHttpRequest();
    ajax.open("GET", "report/"+inputID+"/pdf", true);
    ajax.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                _state.itbResultReportPDF = new Blob([this.response], {type: "application/octet-stream"});
                $('#downloadReportButtonPDF').prop('disabled', false);
                _state.resultLoadPDF.resolve();
            }
        } else if (this.readyState == 2) {
            if (this.status == 200) {
                this.responseType = "blob";
            } else {
                this.responseType = "text";
            }
        }
    };
    ajax.send(null);
	$.when(_state.resultLoadXML, _state.resultLoadPDF).done(function () {
        $.ajax({
            url: "report/"+inputID,
            type: 'DELETE'
        });
	})
}
function downloadReportXML() {
	_state.resultLoadXML.done(function() {
		saveAs(_state.itbResultReportXML, "report.xml");
	});
}
function downloadReportPDF() {
	_state.resultLoadPDF.done(function() {
		saveAs(_state.itbResultReportPDF, "report.pdf");
	});
}
function getLineFromPositionString(positionString) {
    var line = 0, positionParts;
    if (positionString) {
        positionParts = positionString.trim().split(':');
        if (positionParts.length == 3) {
            if (!isNaN(positionParts[1])) {
                line = parseInt(positionParts[1]);
            }
        }
    }
	if (line < 0) {
		line = 0;
	}
	return line;
}
function setCode(reportItemElement) {
	_state.reportLoad.done(function() {
		var editorContent = $('#input-content-pane'),
		    cm;
		editorContent.empty();
		cm = CodeMirror(editorContent[0], {
			value: _state.itbReportData,
			mode:  _config.codeTypeObj,
			lineNumbers: true,
			readOnly: true,
			dragDrop: false
		});
		// Add report messages
		$('.item-info').each(function(index, element) {
		    var line, text, type, indicatorIcon, indicator;
			line = getLineFromPositionString($(this).find('.item-info-location').text());
			text = $(this).find('.item-info-text').text().trim();
			type = 'info';
			indicatorIcon = '<i class="fa fa-info-circle report-item-icon info-icon"></i>';
			if ($(this).find('.item-info-error').length) {
				type = 'error';
				indicatorIcon = '<i class="fa fa-times-circle report-item-icon error-icon"></i>';
			} else if ($(this).find('.item-info-warning').length) {
				type = 'warning';
				indicatorIcon = '<i class="fa fa-exclamation-triangle report-item-icon warning-icon"></i>';
			}
			indicator = $('<div class="indicator-editor-widget indicator-'+type+'">' +
					'<span class="indicator-icon">' +
						indicatorIcon +
					'</span>'+
					'<span class="indicator-desc">' +
						text +
					'</span>' +
				'</div>');
			cm.addLineWidget(line, indicator[0], {
				coverGutter: false,
				noHScroll: true,
				above: true
			});
			cm.getDoc().addLineClass(line, 'background', 'indicator-line-widget');
		});
		$('#input-content-modal').modal('show');
		$('#input-content-modal').on('shown.bs.modal', function() {
			cm.refresh();
			if (reportItemElement) {
			    var line, t, middleHeight;
				line = getLineFromPositionString($(reportItemElement).find('.item-info-location').text());
				cm.getDoc().addLineClass(line, 'background', 'selected-editor-line');
				cm.markText({line: line, ch: 0}, {line: line+1, ch: 0}, {className: 'selected-editor-line-text'});
				t = cm.charCoords({line: line, ch: 0}, "local").top;
				middleHeight = cm.getScrollerElement().offsetHeight / 2;
				cm.scrollTo(null, t - middleHeight - 5);
			}
		});
	});
}