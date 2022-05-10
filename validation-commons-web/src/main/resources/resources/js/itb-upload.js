var _config = {},
    _state = {};
_state.listeners = {};
_state.listenerEvents = {};
_state.listenerEvents['ADDED_EXTERNAL_ARTIFACT_INPUT'] = true;
_state.listenerEvents['REMOVED_EXTERNAL_ARTIFACT_INPUT'] = true;
_state.listenerEvents['RESET_EXTERNAL_ARTIFACT_INPUTS'] = true;
_state.listenerEvents['INPUT_CONTENT_TYPE_CHANGED'] = true;
_state.listenerEvents['VALIDATION_TYPE_CHANGED'] = true;
_state.listenerEvents['FORM_READY'] = true;
_state.listenerEvents['SUBMIT_STATUS_VALIDATED'] = true;
_state.listenerEvents['RESULTS_LOADED'] = true;
_state.contentTypeValidators = {}

$(document).ready(function() {
    registerTemplateHelpers();
	prepareControls();
	if (document.getElementById('text-editor') !== null){
		CodeMirror.fromTextArea(document.getElementById('text-editor'), {
	        mode: _config.codeTypeObj,
	        lineNumbers: true
	    }).on('change', function(){
	    	contentSyntaxChanged();
	    });
	}
    $("body").tooltip({ selector: '[data-toggle=tooltip]' });
    if ($(".panel-heading-details").length > 0) {
        updateSeverityFilterVisibility();
    }
    notifyListeners('FORM_READY', {});
});

function registerTemplateHelpers() {
    const reduceOp = function(args, reducer){
      args = Array.from(args);
      args.pop(); // => options
      var first = args.shift();
      return args.reduce(reducer, first);
    };
    Handlebars.registerHelper({
      'eq': function() { return reduceOp(arguments, function (a,b) { return a === b; }); },
      'ne': function(){ return reduceOp(arguments, function (a,b) { return a !== b; }); },
      'lt': function(){ return reduceOp(arguments, function (a,b) { return a < b; }); },
      'gt': function(){ return reduceOp(arguments, function (a,b) { return a > b; }); },
      'lte': function(){ return reduceOp(arguments, function (a,b) { return a <= b; }); },
      'gte': function(){ return reduceOp(arguments, function (a,b) { return a >= b; }); },
      'and': function(){ return reduceOp(arguments, function (a,b) { return a && b; }); },
      'or': function(){ return reduceOp(arguments, function (a,b) { return a || b; }); },
      'not': function(arg) {
        if (arg) {
            return false;
        }
        return true;
      },
      'sum': function(){ return reduceOp(arguments, function (a,b) { return a + b; }); },
      'switch': function(value, options) {
        this.switch_value = value;
        return options.fn(this);
      },
      'case': function(value, options) {
        if (value == this.switch_value) {
            return options.fn(this);
        }
      },
      'stripNS': function(value) {
        if (value) {
            var start = value.indexOf('}') + 1;
            if (start > 1 && start < value.length) {
                return value.substring(start);
            }
        }
        return value;
      }
    });
}

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
        if (config.custom) {
            _config.custom = config.custom;
        }
        if (config.reportTemplate) {
            _config.reportTemplate = config.reportTemplate;
        } else {
            _config.reportTemplate = 'report.hbs';
        }
        if (config.reportMinimalTemplate) {
            _config.reportMinimalTemplate = config.reportMinimalTemplate;
        } else {
            _config.reportMinimalTemplate = 'reportMinimal.hbs';
        }
    }
}

function addContentTypeValidator(contentType, fn) {
    _state.contentTypeValidators[contentType] = fn;
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

function prepareControls() {
	checkForSubmit();
    var validationTypeSelect,
        types = Object.keys(validationTypeOptions);
    if (types.length == 1) {
        validationTypeSelect = $('#validationType');
        if (validationTypeSelect.length) {
            // Remove the first empty choice and pre-select the single validation type.
            validationTypeSelect.find('option:first').remove();
            validationTypeSelect.val(types[0]);
            validationTypeSelect.attr('disabled', 'disabled');
        }
        validationTypeChanged();
    } else {
        resetExternalArtifacts();
    }
}

function getValidationType() {
    var type, typeToUse,
        typeSelect = $('#validationType');
    if (typeSelect.length) {
        type = typeSelect.val();
    } else {
        // Only one type - no select.
        validationTypes = Object.keys(validationTypeOptions);
        if (validationTypes && validationTypes.length && validationTypes.length > 0) {
            type = validationTypes[0];
        }
    }
    if (type && type != null && type.trim().length != 0) {
        typeToUse = type;
    }
    return typeToUse;
}

function getCompleteValidationType() {
    var typeToUse, option, optionElement,
        type = getValidationType();
    if (type) {
        if (validationTypeOptions[type].length > 0) {
            optionElement = $('#validationTypeOption');
            if (optionElement.length) {
                option = optionElement.val();
            } else {
                // No option select (if only one type and option combination).
                option = validationTypeOptions[type][0].option;
            }
        }
        if (option) {
            typeToUse = type + "." + option;
        } else {
            typeToUse = type;
        }
    }
    return typeToUse;
}

function validationTypeChanged() {
	setValidationOptions();
}

function setValidationOptions() {
    var previousTypeOption, i,
        optionSelected = false,
        type = getValidationType(),
        validationTypeOption = $('#validationTypeOption'),
        validationTypeOptionDiv = $("#validationTypeOptionDiv");
    if (validationTypeOptionDiv.length) {
        if (validationTypeOption.length) {
            previousTypeOption = validationTypeOption.val();
        }
        if (type) {
            if (validationTypeOptions[type].length == 0) {
                // No options - hide option select.
                validationTypeOptionDiv.addClass('hidden');
            } else {
                validationTypeOption.find('option').remove();
                for (i = 0; i < validationTypeOptions[type].length; i++) {
                    if (previousTypeOption && previousTypeOption == validationTypeOptions[type][i].option) {
                        optionSelected = true;
                        validationTypeOption.append('<option value="'+validationTypeOptions[type][i].option+'" selected="true">'+validationTypeOptions[type][i].label+'</option>')
                    } else {
                        validationTypeOption.append('<option value="'+validationTypeOptions[type][i].option+'">'+validationTypeOptions[type][i].label+'</option>')
                    }
                }
                if (!optionSelected) {
                    validationTypeOptionDiv.find('option:first').attr('selected', 'selected');
                }
                if (i == 1) {
                    // Only one option: set as disabled
                    validationTypeOption.attr('disabled', 'disabled');
                } else {
                    validationTypeOption.removeAttr('disabled');
                }
                validationTypeOptionDiv.removeClass('hidden');
            }
        } else {
            // Hide select.
            validationTypeOptionDiv.addClass('hidden');
        }
    }
    validationTypeOptionChanged();
}

function validationTypeOptionChanged() {
    var previousCompleteType = $('#validationTypeToUse').val();
    $('#validationTypeToUse').val(getCompleteValidationType());
	completeValidationTypeChanged(previousCompleteType);
}

function completeValidationTypeChanged(previousValidationType) {
	cleanExternalArtifacts({previousValidationType: previousValidationType});
	checkForSubmit();
	resetExternalArtifacts(previousValidationType);
    notifyListeners('VALIDATION_TYPE_CHANGED', { validationType: getCompleteValidationType() });
}

function cleanExternalArtifacts(options) {
    var i, j, artifactType, removeControls, previousSupportType, newSupportType, elements,
        commonSupport = getCommonExternalArtifactSupport(),
        validationType = getCompleteValidationType();
    for (i = 0; i < _config.artifactTypes.length; i++) {
        artifactType = _config.artifactTypes[i];
        removeControls = true;
        if (!options || !options.force) {
            if (commonSupport[artifactType]) {
                // No need to remove.
                removeControls = false;
            } else if (options && options.previousValidationType) {
                previousSupportType = getExternalArtifactSupport({validationType: options.previousValidationType, artifactType: artifactType});
                newSupportType = getExternalArtifactSupport({validationType: validationType, artifactType: artifactType});
                if (newSupportType == previousSupportType) {
                    // No need to remove.
                    removeControls = false;
                }
            }
        }
        if (removeControls) {
            elements = $(".externalDiv_"+artifactType);
            for (j = 0; j < elements.length; j++){
                removeElement(artifactType, elements[j].getAttribute('id'));
            }
        }
    }
}

function resetExternalArtifacts(previousValidationType) {
    var i, j, artifactType, previousSupport, hasRequired, hasOptional, supportType, updateControls,
        updateCheckbox = false,
        showCheckbox = false,
        artifactTypesWithCommonSupport,
        validationType = getCompleteValidationType(),
        commonSupport = getCommonExternalArtifactSupport();

    if (_config.initialExternalArtifactSetup == undefined) {
        _config.initialExternalArtifactSetup = true;
    }

    artifactTypesWithCommonSupport = Object.keys(commonSupport);
    if (artifactTypesWithCommonSupport.length == _config.artifactTypes.length) {
        // All validation types share the same support for external artifacts.
        if (_config.initialExternalArtifactSetup) {
            updateCheckbox = true;
            hasRequired = false;
            hasOptional = false;
            for (j = 0; j < artifactTypesWithCommonSupport.length; j++) {
                artifactType = artifactTypesWithCommonSupport[j];
                if (commonSupport[artifactType] == 'optional') {
                    hasOptional = true;
                } else if (commonSupport[artifactType] == 'required') {
                   hasRequired = true;
               }
            }
            _config.externalArtifactCheckVisible = hasOptional && !hasRequired
        }
        showCheckbox = _config.externalArtifactCheckVisible;
    } else if (validationType) {
        updateCheckbox = true;
        if (getExternalArtifactSupport({validationType: validationType}) == 'optional') {
            showCheckbox = true;
        }
    }
    if (updateCheckbox) {
        if (showCheckbox) {
            $("#externalArtefactsCheck").prop('checked', false);
            $(".includeExternalArtefacts").removeClass('hidden');
        } else {
            $(".includeExternalArtefacts").addClass('hidden');
        }
    }

    for (i=0; i < _config.artifactTypes.length; i++) {
        artifactType = _config.artifactTypes[i];
        supportType = 'none';
        updateControls = true;
        if (commonSupport[artifactType]) {
            supportType = commonSupport[artifactType];
            updateControls = _config.initialExternalArtifactSetup;
        } else if (validationType) {
            // A validation type is defined.
            supportType = getExternalArtifactSupport({validationType: validationType, artifactType: artifactType});
            if (!_config.initialExternalArtifactSetup && previousValidationType) {
                previousSupport = getExternalArtifactSupport({validationType: previousValidationType, artifactType: artifactType});
                updateControls = supportType != previousSupport
            }
        }
        if (updateControls) {
            if (supportType == 'required') {
                addElement(artifactType, placeholderTextForExternalArtifactFile(artifactType), false);
                $('#rmvButton-external_'+artifactType+'-1').addClass('hidden');
                $('#fileToValidate-class-external_'+artifactType+'-1').removeClass('col-sm-11');
                $('#fileToValidate-class-external_'+artifactType+'-1').addClass('col-sm-12');
                $('#uriToValidate-external_'+artifactType+'-1').removeClass('col-sm-11');
                $('#uriToValidate-external_'+artifactType+'-1').addClass('col-sm-12');
            }
            if (supportType == 'none' || showCheckbox) {
                $('.externalClass_'+artifactType).addClass('hidden');
            } else {
                $('.externalClass_'+artifactType).removeClass('hidden');
            }
        }
    }
    _config.initialExternalArtifactSetup = false;
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
	toggleExt = getExternalArtifactSupport({artifactType: artifactType}),
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
	var toggleExt = getExternalArtifactSupport({artifactType: artifactType}),
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
    updateSubmitStatus();
	notifyListeners('SUBMIT_STATUS_VALIDATED', {});
}

function updateSubmitStatus() {
	var type = $('#contentType').val(),
	    inputType = $('#validationType'),
	    inputTypeOption = $('#validationTypeOption'),
	    submitDisabled = true,
	    inputFile, uriInput, stringType, i;
	$('#inputFileSubmit').prop('disabled', true);
    submitDisabled = ((!inputType.length || inputType.val()) && (!inputTypeOption.length || inputTypeOption.val()))?false:true
    if (!submitDisabled) {
        if (type == "fileType") {
            inputFile = $("#inputFileName");
            submitDisabled = inputFile.val()?false:true
        } else if (type == "uriType") {
            uriInput = $("#uri");
            submitDisabled = uriInput.val()?false:true
        } else if (type == "stringType") {
            stringType = getCodeMirrorNative('#text-editor').getDoc();
            submitDisabled = stringType.getValue()?false:true
        } else if (_state.contentTypeValidators[type]) {
            submitDisabled = !_state.contentTypeValidators[type]();
        }
        if (!submitDisabled) {
            for (i=0; i < _config.artifactTypes.length; i++) {
                if (getExternalArtifactSupport({artifactType: _config.artifactTypes[i]}) == 'required') {
                    submitDisabled = !externalElementHasValue(document.getElementsByName("contentType-external_"+_config.artifactTypes[i]));
                    if (submitDisabled) {
                        break;
                    }
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
    var artifactSupport, i, ext,
        checked = $("#externalArtefactsCheck").is(":checked");
    if (checked) {
        for (i=0; i < _config.artifactTypes.length; i++) {
            ext = getExternalArtifactSupport({artifactType: _config.artifactTypes[i]});
            if (ext == "none") {
                $(".externalClass_"+_config.artifactTypes[i]).addClass("hidden");
            } else {
                $(".externalClass_"+_config.artifactTypes[i]).removeClass("hidden");
            }
        }
        if (_config.artifactTypes.length == 1) {
            artifactSupport = getExternalArtifactSupport({artifactType: _config.artifactTypes[0]});
            if (artifactSupport != "none") {
                addExternal(_config.artifactTypes[0]);
            }
        }
    } else {
        for (i=0; i < _config.artifactTypes.length; i++) {
            $(".externalClass_"+_config.artifactTypes[i]).addClass("hidden");
        }
        cleanExternalArtifacts({force: true});
    }
}

function getCommonExternalArtifactSupport() {
    var supportType, supportTypes, resultingSupportTypes, i, j, k, artifactType, validationTypes, validationType, options, completeType, artifactTypeSupportTypes;
    if (!_config.commonExternalArtefactsSupportSet) {
        supportTypes = {};
        resultingSupportTypes = {};
        for (i = 0; i < _config.artifactTypes.length; i++) {
            artifactType = _config.artifactTypes[i];
            supportTypes[artifactType] = {};
        }
        validationTypes = Object.keys(validationTypeOptions);
        for (i = 0; i < validationTypes.length; i++) {
            validationType = validationTypes[i];
            if (validationTypeOptions[validationType].length == 0) {
                for (j = 0; j < _config.artifactTypes.length; j++) {
                    artifactType = _config.artifactTypes[j];
                    supportType = _config.externalArtifacts[validationType][artifactType];
                    supportTypes[artifactType][supportType] = true;
                }
            } else {
                options = validationTypeOptions[validationType];
                for (j = 0; j < options.length; j++) {
                    completeType = validationType + '.' + options[j].option
                    for (k = 0; k < _config.artifactTypes.length; k++) {
                        artifactType = _config.artifactTypes[k];
                        supportType = _config.externalArtifacts[completeType][artifactType];
                        supportTypes[artifactType][supportType] = true;
                    }
                }
            }
        }
        for (i = 0; i < _config.artifactTypes.length; i++) {
            artifactType = _config.artifactTypes[i];
            artifactTypeSupportTypes = Object.keys(supportTypes[artifactType]);
            if (artifactTypeSupportTypes.length == 1) {
                resultingSupportTypes[artifactType] = artifactTypeSupportTypes[0];
            }
        }
        _config.commonExternalArtefactsSupport = resultingSupportTypes;
        _config.commonExternalArtefactsSupportSet = true;
    }
    return _config.commonExternalArtefactsSupport;
}

function getExternalArtifactSupport(options) {
	var i, currentArtifactType, artifactType,
	    supportType = "none",
	    type = getCompleteValidationType(),
	    commonSupport = getCommonExternalArtifactSupport();
	if (options) {
	    if (options.validationType) {
	        type = options.validationType
	    }
	}
	if (type) {
        if (options.artifactType) {
            supportType = _config.externalArtifacts[type][options.artifactType];
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
	} else {
	    if (options.artifactType) {
	        if (commonSupport[options.artifactType]) {
	            supportType = commonSupport[options.artifactType];
	        }
	    } else {
            for (i = 0; i < _config.artifactTypes.length; i++) {
                artifactType = _config.artifactTypes[i];
                if (commonSupport[artifactType]) {
                    if (commonSupport[artifactType] == "required") {
                        supportType = "required";
                        break;
                    } else if (commonSupport[artifactType] == "optional") {
                        supportType = "optional";
                    }
                }
            }
	    }
	}
    return supportType;
}

function getReportData(inputID) {
    _state.reportLoad = jQuery.Deferred();
    _state.resultLoadXML = jQuery.Deferred();
    _state.resultLoadXMLAggregate = jQuery.Deferred();
    _state.resultLoadPDF = jQuery.Deferred();
    _state.resultLoadPDFAggregate = jQuery.Deferred();
    _state.resultLoadCSV = jQuery.Deferred();
    _state.resultLoadCSVAggregate = jQuery.Deferred();
    $(document).ready(function() {
        getReport(inputID);
        getResultReport(inputID);
    });
}
function getReport(inputID) {
    if ($('#viewInputButton').length) {
        $.get("input/"+inputID, function(data) {
            _state.itbReportData = data;
            $.ajax({
                url: "input/"+inputID,
                type: 'DELETE',
                beforeSend: function(request) {
                	request.setRequestHeader("X-Requested-With", "XMLHttpRequest");
                },
                error: function(response){
                	raiseAlert(response)
                }
            });
            _state.reportLoad.resolve();
            $('#viewInputButtonSpinner').addClass('hidden');
            $('#viewInputButton').prop('disabled', false);
        });
    }
}
function getResultReportForReportType(inputID, pathPostfix, aggregate, buttonID, spinnerID) {
    var resultPromise = jQuery.Deferred();
    if ($('#'+buttonID).length) {
    	 var request = new XMLHttpRequest();
    	 request.open("GET", "report/"+inputID+"/"+pathPostfix+"?aggregate="+aggregate, true);
    	 request.setRequestHeader("X-Requested-With", "XMLHttpRequest");
         request.onreadystatechange = function() {
             if (request.readyState == 4) {
                 if (request.status == 200) {
                    $('#'+spinnerID).addClass('hidden');
                    $('#'+buttonID).removeClass('disabled');
                    resultPromise.resolve({ payload: request.response, ok: true });
                 } else if (request.responseText != "") {
                 	_state.error = JSON.parse(request.responseText).errorMessage;
                 	raiseAlert(_state.error);
                 	resultPromise.resolve();
 				}
             } else if (request.readyState == 2) {
                 if (request.status == 200) {
                     request.responseType = "blob";
                 } else {
                     request.responseType = "text";
                 }
             }
         };
         request.send(null);
    } else {
        resultPromise.resolve();
    }
    return resultPromise
}
function getResultReport(inputID) {
    getResultReportForReportType(inputID, 'xml', false, 'downloadReportButtonXML', 'downloadReportButtonXMLSpinner').done(function (data) {
        if (data && data.ok) _state.itbResultReportXML = new Blob([data.payload], { type: 'application/xml' });
        _state.resultLoadXML.resolve();
    })
    getResultReportForReportType(inputID, 'xml', true, 'downloadReportButtonXMLAggregate', 'downloadReportButtonXMLSpinnerAggregate').done(function (data) {
        if (data && data.ok) _state.itbResultReportXMLAggregate = new Blob([data.payload], { type: 'application/xml' });
        _state.resultLoadXMLAggregate.resolve();
    })
    getResultReportForReportType(inputID, 'pdf', false, 'downloadReportButtonPDF', 'downloadReportButtonPDFSpinner').done(function (data) {
        if (data && data.ok) _state.itbResultReportPDF = new Blob([data.payload], {type: "application/octet-stream"});
       _state.resultLoadPDF.resolve();
    })
    getResultReportForReportType(inputID, 'pdf', true, 'downloadReportButtonPDFAggregate', 'downloadReportButtonPDFSpinnerAggregate').done(function (data) {
        if (data && data.ok) _state.itbResultReportPDFAggregate = new Blob([data.payload], {type: "application/octet-stream"});
        _state.resultLoadPDFAggregate.resolve();
    })
    getResultReportForReportType(inputID, 'csv', false, 'downloadReportButtonCSV', 'downloadReportButtonCSVSpinner').done(function (data) {
        if (data && data.ok) _state.itbResultReportCSV = new Blob([data.payload], {type: "application/octet-stream"});
        _state.resultLoadCSV.resolve();
    })
    getResultReportForReportType(inputID, 'csv', true, 'downloadReportButtonCSVAggregate', 'downloadReportButtonCSVSpinnerAggregate').done(function (data) {
        if (data && data.ok) _state.itbResultReportCSVAggregate = new Blob([data.payload], {type: "application/octet-stream"});
        _state.resultLoadCSVAggregate.resolve();
    })
	$.when(_state.resultLoadXML, _state.resultLoadXMLAggregate, _state.resultLoadPDF, _state.resultLoadPDFAggregate, _state.itbResultReportCSV, _state.itbResultReportCSVAggregate).done(function () {
        $.ajax({
            url: "report/"+inputID,
            type: 'DELETE',
            beforeSend: function(request) {
            	request.setRequestHeader("X-Requested-With", "XMLHttpRequest");
            },
            error: function(response){
            	raiseAlert(response)
            }
        });
	})
}
function clearMessages() {
    $('#messagePlaceholder').addClass('hidden');
    $('#messagePlaceholder').empty();
}
function raiseAlert(errorInfo, isFinal) {
    var message = 'An unexpected error occurred';
    if (isFinal) {
        message = errorInfo;
    } else {
        try {
            if (errorInfo) {
                if (errorInfo.responseJSON && errorInfo.responseJSON.errorMessage) {
                    message = errorInfo.responseJSON.errorMessage;
                } else if (errorInfo.responseText) {
                    message = JSON.parse(request.responseText).errorMessage;
                }
            }
        } catch (error) {
            message = 'An unexpected error occurred';
        }
    }
    $('#messagePlaceholder').text(message);
    $('#messagePlaceholder').removeClass('hidden');
}
function downloadReportXML() {
	_state.resultLoadXML.done(function() {
		saveAs(_state.itbResultReportXML, "report.xml");
	});
}
function downloadReportXMLAggregate() {
	_state.resultLoadXMLAggregate.done(function() {
		saveAs(_state.itbResultReportXMLAggregate, "report.xml");
	});
}
function downloadReportPDF() {
	_state.resultLoadPDF.done(function() {
		saveAs(_state.itbResultReportPDF, "report.pdf");
	});
}
function downloadReportPDFAggregate() {
	_state.resultLoadPDFAggregate.done(function() {
		saveAs(_state.itbResultReportPDFAggregate, "report.pdf");
	});
}
function downloadReportCSV() {
	_state.resultLoadCSV.done(function() {
		saveAs(_state.itbResultReportCSV, "report.csv");
	});
}
function downloadReportCSVAggregate() {
	_state.resultLoadCSVAggregate.done(function() {
		saveAs(_state.itbResultReportCSVAggregate, "report.csv");
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
		$('#reportItemsDetailed .item-info').each(function(index, element) {
		    var line, lineToHighlight, text, type, indicatorIcon, indicator;
			line = getLineFromPositionString($(this).find('.item-info-location').text());
			lineToHighlight = line - 1
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
			cm.addLineWidget(lineToHighlight, indicator[0], {
				coverGutter: false,
				noHScroll: true,
				above: true
			});
			cm.getDoc().addLineClass(lineToHighlight, 'background', 'indicator-line-widget');
		});
		$('#input-content-modal').modal('show');
		$('#input-content-modal').on('shown.bs.modal', function() {
			cm.refresh();
			if (reportItemElement) {
			    var line, lineToHighlight, t, middleHeight;
				line = getLineFromPositionString($(reportItemElement).find('.item-info-location').text());
    			lineToHighlight = line - 1
				cm.getDoc().addLineClass(lineToHighlight, 'background', 'selected-editor-line');
				cm.markText({line: lineToHighlight, ch: 0}, {line: lineToHighlight+1, ch: 0}, {className: 'selected-editor-line-text'});
				t = cm.charCoords({line: lineToHighlight, ch: 0}, "local").top;
				middleHeight = cm.getScrollerElement().offsetHeight / 2;
				cm.scrollTo(null, t - middleHeight - 5);
			}
		});
	});
}
function localeChanged() {
    var selectedLocale = $('select#availableLocales option:checked').val(),
        url = window.location.href;
    $('select#availableLocales [selected=true]').attr("selected", "false");
    $('select#availableLocales option:checked').attr("selected", "true");
    if (url.indexOf('?') > -1){
        url = url.split('?')[0];
    }
    url += '?lang=' + selectedLocale; 
    window.location.href = url;
}

function reportTypeChange(aggregate) {
    if (aggregate) {
        $('#reportItemsAggregated').removeClass('hidden');
        $('#reportItemsDetailed').addClass('hidden');
        $('#reportTypeSelectText').text($('#reportTypeSelectAggregatedText').text());
    } else {
        $('#reportItemsAggregated').addClass('hidden');
        $('#reportItemsDetailed').removeClass('hidden');
        $('#reportTypeSelectText').text($('#reportTypeSelectDetailedText').text());
    }
    updateSeverityFilterVisibility();
}

function updateSeverityFilterVisibility() {
    var parentId = 'reportItemsDetailed';
    if ($('#reportItemsDetailed').hasClass('hidden')) {
        parentId = 'reportItemsAggregated';
    }
    var errorCount = $('#'+parentId+' .report-item.report-item-error').length,
        warningCount = $('#'+parentId+' .report-item.report-item-warning').length,
        messageCount = $('#'+parentId+' .report-item.report-item-info').length,
        visibleCount = 0
    if (errorCount == 0) {
        $('#severityFilterErrorOption').addClass('hidden');
    } else {
        $('#severityFilterErrorOption').removeClass('hidden');
        visibleCount += 1;
    }
    if (warningCount == 0) {
        $('#severityFilterWarningOption').addClass('hidden');
    } else {
        $('#severityFilterWarningOption').removeClass('hidden');
        visibleCount += 1;
    }
    if (messageCount == 0) {
        $('#severityFilterMessageOption').addClass('hidden');
    } else {
        $('#severityFilterMessageOption').removeClass('hidden');
        visibleCount += 1;
    }
    if (visibleCount > 1) {
        $('#severityFilterButton').removeClass('hidden');
    } else {
        $('#severityFilterButton').addClass('hidden');
    }
}

function severityFilterChange(level) {
    if (level == 'error') {
        $('#severityFilterText').text($('#severityFilterShowErrorsText').text());
        $('.report-item.report-item-error').removeClass('hidden');
        $('.report-item.report-item-warning').addClass('hidden');
        $('.report-item.report-item-info').addClass('hidden');
    } else if (level == 'warning') {
        $('#severityFilterText').text($('#severityFilterShowWarningsText').text());
        $('.report-item.report-item-error').addClass('hidden');
        $('.report-item.report-item-warning').removeClass('hidden');
        $('.report-item.report-item-info').addClass('hidden');
    } else if (level == 'message') {
        $('#severityFilterText').text($('#severityFilterShowMessagesText').text());
        $('.report-item.report-item-error').addClass('hidden');
        $('.report-item.report-item-warning').addClass('hidden');
        $('.report-item.report-item-info').removeClass('hidden');
    } else {
        $('#severityFilterText').text($('#severityFilterShowAllText').text());
        $('.report-item.report-item-error').removeClass('hidden');
        $('.report-item.report-item-warning').removeClass('hidden');
        $('.report-item.report-item-info').removeClass('hidden');
    }
}

function doSubmit() {
    $('#reportPlaceholder').empty();
    clearMessages();
	waitingDialog.show(validatingInputMessage, {dialogSize: 'm'}, _config.isMinimalUI?'busy-modal-minimal':'busy-modal');
	var form = $('form:first');
    $.ajax({
        url: $(form).prop("action"),
        type: 'POST',
        data: new FormData(form[0]),
        success: function (data) {
            waitingDialog.hide();
            displayReport(data);
        },
        error: function (response) {
            waitingDialog.hide();
            raiseAlert(response);
        },
        cache: false,
        contentType: false,
        processData: false
    });
    return false;
}

function displayReport(data) {
    if (data.message) {
        // An error occurred.
        $('#messagePlaceholder').text(data.message);
        $('#messagePlaceholder').removeClass('hidden');
    } else {
        // Validation completed.
        var params = {
            data: data,
            config: _config
        };
        if (_config.isMinimalUI) {
            _config.titleTextBackup = $('.panel-title-ui').text();
            $('.panel-body-minimal').addClass('hidden');
            $('#inputFileSubmitMinimal').addClass('hidden');
            $('#backSubmit').removeClass('hidden');
            $('form').removeClass('form-horizontal');
            $('.panel-title-ui').text(data.translations.resultSectionTitle);
            $('#reportPlaceholder').append($(App.Templates[_config.reportMinimalTemplate](params)));
        } else {
            $('#reportPlaceholder').append($(App.Templates[_config.reportTemplate](params)));
        }
        getReportData(data.reportId);
        notifyListeners('RESULTS_LOADED', {data: data});
    }
}

function doBack() {
    $('.panel-body-minimal').removeClass('hidden');
    $('#inputFileSubmitMinimal').removeClass('hidden');
    $('#backSubmit').addClass('hidden');
    $('form').addClass('form-horizontal');
    $('.panel-title-ui').text(_config.titleTextBackup);
    $('#reportPlaceholder').empty();
}

function toggleReportDisplay() {
    var reportOverviewDiv = $('#reportSummary');
    var reportDetailsDiv = $('#reportItemsContent');
    var viewDetailsButton = $('#viewDetailsButton');
    var viewSummaryButton = $('#viewSummaryButton');
    var overviewToolbar = $('#reportOverviewButtons');
    var detailsToolbar = $('#reportDetailsButtons');
    if (reportDetailsDiv.hasClass('hidden')) {
        // Show report details
        reportDetailsDiv.removeClass('hidden');
        reportOverviewDiv.addClass('hidden');
        viewSummaryButton.removeClass('hidden');
        viewDetailsButton.addClass('hidden');
        overviewToolbar.addClass('hidden');
        detailsToolbar.removeClass('hidden');
    } else {
        // Show report overview
        reportDetailsDiv.addClass('hidden');
        reportOverviewDiv.removeClass('hidden');
        viewSummaryButton.addClass('hidden');
        viewDetailsButton.removeClass('hidden');
        overviewToolbar.removeClass('hidden');
        detailsToolbar.addClass('hidden');
    }
}