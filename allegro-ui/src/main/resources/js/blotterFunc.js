
  var tableRef 			= document.getElementById('blotter').getElementsByTagName('tbody')[0];
  var heartbeatRef 		= document.getElementById('heartbeat');
  var errorRef 			= document.getElementById('error');
  var header 			= document.getElementById('header');
  var editContainer 	= document.getElementById('editContainer');
  var insertContainer 	= document.getElementById('insertContainer');
  var chooserContainer 	= document.getElementById('chooserContainer');
  var insertPanel 		= document.getElementById('insertPanel');
  var insertError 		= document.getElementById('insertError');
  var editButtonBar 	= document.getElementById('editButtonBar');
  var pageContent 		= document.getElementById('pageContent');
  var configureMenu 	= document.getElementById('configure-menu');
  var chooserTarget		= null;
  var schema		 	= null;
  var threadId		 	= "";
  
  function heartBeat(time)
  {
	 heartbeatRef.innerHTML = time; 
  }
  
  function showError(message)
  {
	 errorRef.innerHTML = message; 
  }
  
  function addCol(newRow, text)
  {
    // Insert a cell in the row at index 0
    var newCell  = newRow.insertCell(-1);
    // Append a text node to the cell
    var newText  = document.createTextNode(text);
    newCell.appendChild(newText);
  }
  
  function upsert(newRow, storedObject)
  {
    addCol(newRow, storedObject.sortKey);
    
    if(storedObject.header == null)
  	  addCol(newRow, '');
    else
    	addCol(newRow, storedObject.header._type);
  }
  
  function deletePayload(baseHash)
  {
	var row = document.getElementById(baseHash);
	
	if(row != null)
	{
		tableRef.deleteRow(row.rowIndex);
	}
  }

  function render(rowId, rowThreadId, attributes)
  {
	if(threadId == "" && rowThreadId != null)
	{
		threadId = rowThreadId;
	}
	
	var row = document.getElementById(rowId);
	
	if(row == null)
	{
	    // Insert a row in the table at the last row
	    row = tableRef.insertRow(-1);
	    row.id = rowId;
	    $(row).css("position", "relative");
	    row.insertCell(-1);
	    
	}
	
	var controls = "";
    
    if(!absolute)
    	controls += 
    		"<a  style=\"text-decoration: none;\" href=\"javascript:deleteRow('" +
//    		"<button class=\"error rowAction\" onclick=\"cell = deleteRow('" +
    		rowId +
    	"', '" + attributes['Absolute Hash'].editText +
    	"')\"><b class=\"error icon rowAction\">&times;</b></a>";
    
    row.cells[0].innerHTML = controls;
	
    for(i=1 ; i<header.cells.length ; i++)
    {
    	var headerCell = header.cells[i];
    	var attr = attributes[headerCell.innerHTML];
    	var cell;
    	
    	if(row.cells.length > i)
    		cell = row.cells[i];
    	else
    		cell = row.insertCell(-1);
    	
    	if(headerCell.id != "")
    		cell.classList.add(headerCell.id);
    	cell.style.display = headerCell.style.display;
    	$(cell).data('schema', attr);
    	
    	
    	if(attr == null)
    	{
    		cell.innerHTML = '';
    	}
    	else
    	{
    		renderCell(cell, attr);
    		delete attributes[headerCell.innerHTML];
    	}
    }
    
    for(var key in attributes)
    {
    	var attr = attributes[key];
    	var attrId = key.split(' ').join('_');
    	
    	
    	var colId = 'COL_' + attrId;
    	var cell = document.createElement("th");

        header.appendChild(cell);
    	
    	cell.id = colId;
    	cell.innerHTML = key;
    	$(cell).data('schema', attr);
    	
    	var cell = row.insertCell(-1);
    	cell.classList.add(colId);
    	$(cell).data('schema', attr);
    	
    	renderCell(cell, attr);
    	
    	var menu = document.getElementById('configure-menu-list');
    	
    	var checkBox = document.createElement("input");

    	checkBox.id = 'CHECK_' + attrId;
    	checkBox.attrId = attrId;
    	checkBox.type = 'checkbox';
    	
    	if(attr.hiddenByDefault)
    	{
    		cell.style.display = "none";
        	checkBox.checked = false;
    	}
    	else
    	{
        	checkBox.checked = true;
    	}
    	checkBox.onchange = function(){toggleCol(this.attrId);};
    	
    	var textNode = document.createTextNode(key);
    	
    	var li = document.createElement("li");
    	
    	li.append(checkBox);
    	li.append(textNode);
    	
    	menu.append(li);
    }
  }
  
  function renderCell(cell, attr)
  {
	if(attr.hoverText != null)
	{
		cell.innerHTML = '<span title="' + attr.hoverText + '">' + attr.text + '</span>';
	}
	else
	{
		cell.innerHTML = attr.text;
	}
  }

  function upsertPayload(baseHash, storedObject, payload)
  {
	var row = document.getElementById(baseHash);
	
	if(row == null)
	{
	    // Insert a row in the table at the last row
	    var newRow   = tableRef.insertRow(-1);
	    
	    newRow.id = baseHash;
	    upsert(newRow, storedObject);
	    
	    addCol(newRow, payload._type);
	    addCol(newRow, payload.description);
	}
	else
	{
		row.cells[0].innerHTML = storedObject.sortKey;
		
		if(storedObject.header == null)
			row.cells[1].innerHTML = storedObject.sortKey;
		else
			row.cells[1].innerHTML = storedObject.header._type;
		
		row.cells[2].innerHTML = payload._type;
		row.cells[3].innerHTML = payload.description;
	}
  }
  
  function hideCol(key)
  {
	  showHideCol(key, "none");
  }
  
  function showCol(key)
  {
	  showHideCol(key, "table-cell");
  }
  
  function showHideCol(key, display)
  {
	  var colId = 'COL_' + key.split(' ').join('_');
	  var allCol=document.getElementsByClassName(colId);
	  for(var i=0;i<allCol.length;i++)
	  {
	   allCol[i].style.display = display;
	  }
	  document.getElementById(colId).style.display = display;
  }
  
  function toggleCol(key)
  {
	  var colId = 'COL_' + key.split(' ').join('_');
	  var display = "none";
	  
	  if(document.getElementById(colId).style.display == "none")
		  display = "table-cell";
	  
	  console.log('toggleCol ' + colId + ', ' + display);
	  
	  var allCol=document.getElementsByClassName(colId);
	  for(var i=0;i<allCol.length;i++)
	  {
	   allCol[i].style.display = display;
	  }
	  
	  document.getElementById(colId).style.display = display;
  }
  
  function showMenu(id)
  {
	  //document.getElementById(id).classList.toggle("show");
	  $(document.getElementById(id)).dialog({
	      resizable: false,
	      height: "auto",
	      width: "auto",
	      modal: true
	    });
  }
  
  window.onclick = function(event)
  {
	if(configureMenu.classList.contains('show'))
	{
		//Close the dropdown menu if the user clicks outside of it
		if (!event.target.matches('.dropbtn')) {
			configureMenu.classList.remove('show');
		}
	}
	else
	{
		var existingEditPanel = document.getElementById("editPanel");
		
		if(existingEditPanel == null)
		{
			if(!event.target.matches('.rowAction') && !event.target.closest('tr').matches('.rowAction'))
			{
			  if (event.target.closest('#blotter'))
				openEditPanel(event);
			}
		}
	}
  }
  
  function saveComplete(data, textStatus, jqXHR)
  {
	  //alert('data=' + data + ', textStatus=' + textStatus + ', jqXHR=' + jqXHR);
  }
  
  function saveFailed(jqXHR, textStatus, error)
  {
	  alert('Save Failed: ' + error);
  }
  
  function editSave()
  {
    // do the save operation
	$.post("/edit", $( "#editForm" ).serialize(), saveComplete).fail(saveFailed);
//	document.getElementById('editForm').submit();
	// now close the dalog
	  editCancel();
  }
  
  function editCancel()
  {
	var existingEditPanel = document.getElementById("editPanel");
	
	if(existingEditPanel != null)
	{
		$(existingEditPanel).remove();
	}
	editContainer.style.display='none';
  }
  
  function chooseThreadId(target)
  {
	  chooserTarget = target.form;
	  $("#chooserTitle").html("Choose Thread");
	  $.get("/chooseStreams", populateChooser);
	  //chooserContainer.style.display='block';
	  
	  $(chooserContainer).dialog({
	      resizable: false,
	      height: "auto",
	      width: "auto",
	      modal: true,
	      buttons: {
	        "Choose": function() {

	    	  	chooserSave();
	    	  
	        	$( this ).dialog( "close" );
	        },
	        Cancel: function() {
	          chooserCancel();
	          $( this ).dialog( "close" );
	        }
	      }
	    });
  }
  
  function populateChooser(data)
  {
	  $(data).appendTo('#chooserPanel');
  }
  
  function chooserSave()
  {
    // do the save operation
	if(chooserTarget != null)
	{
		$(chooserTarget).find('input[name="threadId"]').val(
			$("#chooserForm").find('input[name="result"]:checked').val()
			);
		chooserTarget = null;
	}
	// now close the dalog
	chooserCancel();
  }
  
  function chooserCancel()
  {
	var existingEditPanel = document.getElementById("chooserPanel");
	
	if(existingEditPanel != null)
	{
		$(existingEditPanel).empty();
	}
//	chooserContainer.style.display='none';
  }

  function openInsertPanel()
  {
	  insertError.innerHTML = '';
	  $("#insertForm input[name='threadId']").val(threadId)
	  //insertContainer.style.display='block';
	  $(insertContainer).dialog({
	      resizable: false,
	      height: "auto",
	      width: "auto",
	      modal: true,
	      buttons: {
	        Save: function() {

	    	  	var o = {};
	    		var a = $("#insertAttributesForm").serializeArray();
	    		$.each(a, function() {
	    			if (o[this.name]) {
	    				if (!o[this.name].push) {
	    					o[this.name] = [o[this.name]];
	    				}
	    				o[this.name].push(this.value || '');
	    			} else {
	    				o[this.name] = this.value || '';
	    			}
	    		});
	    	
	    	  $("#insertForm input[name='payload']").val(JSON.stringify(o))
	    	  
	    	  $.post("/insert", $("#insertForm").serialize(), insertComplete).fail(insertFailed);
	    	  
	        	$( this ).dialog( "close" );
	        },
	        Cancel: function() {
	          $( this ).dialog( "close" );
	        }
	      }
	    });
  }
  
  function insertAddAttribute()
  {
	  let newAttributeName = $("#insertAddAttributeForm").find('input[name="attributeName"]').val();
	  let form = document.getElementById('insertForm');
	  
	  for(var i=0; i < form.elements.length; i++)
	  {
		if(form.elements[i].name == newAttributeName)
		{
			insertError.innerHTML = 'Attribute ' + newAttributeName + ' already exists.';
		    return;
		}
	  }

	  $("<tr id=\"INSERT_" + newAttributeName + "\"><td>" + newAttributeName + "</td><td><input type=text name=\"" + newAttributeName + 
			"\"></td><td><button onclick=insertRemoveAttribute('" + newAttributeName + 
	  		"')>Remove</button></td></tr>")
	  	.appendTo($("#insertPanel"));
	  insertError.innerHTML = '';
  }
  
  function insertRemoveAttribute(id)
  {
	  $("#INSERT_" + id).remove();
  }
  
  function insertSave()
  {
	  // do the save operation
	  	var o = {};
		var a = $("#insertAttributesForm").serializeArray();
		$.each(a, function() {
			if (o[this.name]) {
				if (!o[this.name].push) {
					o[this.name] = [o[this.name]];
				}
				o[this.name].push(this.value || '');
			} else {
				o[this.name] = this.value || '';
			}
		});
	
	  $("#insertForm input[name='payload']").val(JSON.stringify(o))
	  
	  $.post("/insert", $("#insertForm").serialize(), insertComplete).fail(insertFailed);
//		document.getElementById('insertForm').submit();
	  // now close the dalog
	  insertCancel();
  }

  function insertComplete(data, textStatus, jqXHR)
  {
	  //alert('Insert data=' + data + ', textStatus=' + textStatus + ', jqXHR=' + jqXHR);
  }
  
  function insertFailed(jqXHR, textStatus, error)
  {
	  alert('Insert Failed: ' + error);
  }
  
  function insertCancel()
  {
    insertContainer.style.display='none';
  }
  
  function deleteRow(rowId, absoluteHash)
  {
	  var row = document.getElementById(rowId);
	
	  if(row != null)
	  {
		if(confirm('Delete object ' + rowId + '?'))
		{
			var payload = {};
			payload.absoluteHash = absoluteHash;

			var deleteComplete = function(rowIndex)
			{
				return function(data, textStatus, jqXHR)
				{
					tableRef.deleteRow(rowIndex);
					//alert('Delete data=' + data + ', textStatus=' + textStatus + ', jqXHR=' + jqXHR);
			    };
			};
			$.post("/delete", payload, deleteComplete(row.rowIndex)).fail(deleteFailed);
			
		}
	  }
  }
  
  function deleteFailed(jqXHR, textStatus, error)
  {
	  alert('Delete Failed: ' + error);
  }
  
  function openEditPanel(event)
  {
	var trElement = event.target.closest('tr');
		
	$(editContainer).width(trElement.scrollWidth); 
	editContainer.style.display='block';
  

	  var pos = $(trElement).offset();
	  pos.top -= parseInt($(pageContent).css("margin-top"));
	  pos.left -= parseInt($(pageContent).css("margin-left"));
	  
	  var editPanel = 
		  $("<div id=\"editPanel\"></div>")
		  .css({
		    position: "absolute",
		    width: '100%',
		    height: $(trElement).height(),
		    top:  pos.top,
		    left: pos.left,

		    background: "#e8e8e8",
		    
		  })
		  ;
	  
	  let buttonPos = $(editButtonBar).offset();
	  buttonPos.top = $(trElement).offset().top + $(trElement).height();
	  
	  //$(editButtonBar).css("background", "#e8e8e8");
//	  pos.top += $(editPanel).height();
	  $(editButtonBar).offset(buttonPos);
	  
	  //$(editPanel).css("background", "transparent");
	  
	  editPanel.appendTo(editContainer);
	  var editForm = $("<form id=\"editForm\" method=\"post\" action=\"/edit\"></form>");
	  
	  editForm.appendTo(editPanel);
	  
	  $(window).resize(function () {
		  var editPanel = document.getElementById("editPanel");
		  
		  if(editPanel != null)
		  {
			$(editContainer).width(trElement.scrollWidth);
			var editForm = document.getElementById("editForm");
			var x=0;
			
			for(i=0 ; i<1 ; i++)
			{
				var headerCell = header.cells[i];
				x += $(headerCell).outerWidth(true);
			}
			
			for(i=0 ; i<editForm.children.length ; i++)
			{
				var editor = editForm.children[i];
				var headerCell = header.cells[i+1];
				
				$(editor).css({
				    width: $(headerCell).outerWidth(false),
				    height: $(editPanel).height(),
				    top: 0,
				    left: x
				})
				x += $(headerCell).outerWidth(true);
			}
		  }
		  //var trElement = $("#editPanel").closest('tr');
		  
	  })
	  
	  var x=0;
	  
	  for(i=0 ; i<1 ; i++)
	  {
		  var headerCell = header.cells[i];
		  x += $(headerCell).outerWidth(true);
	  }
	  for(i=1 ; i<header.cells.length ; i++)
	  {
		var cell = trElement.cells[i];
		var headerCell = header.cells[i];
		
		if(cell == null)
		{
			cell = trElement.insertCell(i);
			cell.style.display = headerCell.style.display
		}
		
		if(cell.style.display != "none")
		{
//				$(cell).height();
			var editor;
			
			var attrSpec = $(cell).data("schema");
			var value;
			
			if(attrSpec == null)
			{
				attrSpec = $(headerCell).data("schema");
				value = "";
			}
			else
			{
				value = attrSpec.editText;
				if(value == null)
					value = attrSpec.text;
				if(value == null)
					value = "";
			}
			
			if(attrSpec != null)
			{
				editor = $("<textarea id=\"EDIT_" + headerCell.id + "\", name=\"" + attrSpec.id + "\">" + 
						value + "</textarea>");
				editor.attrId = headerCell.id;
				editor.id = 'EDIT_' + headerCell.id;
				
				if(!attrSpec.editable)
				{
					$(editor).prop('readonly', true);
					$(editor).css("background", "#e8e8e8");
				}
	
		    	editor.css({
				    position: "absolute",
				    width: $(cell).outerWidth(false),
				    height: $(editPanel).height(),
				    top: 0,
				    left: x
				}).appendTo(editForm);
		    	//.css("position", "relative");
			}			
	    	x += $(cell).outerWidth(true);
		}
	  }
  }
