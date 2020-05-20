
  var tableRef = document.getElementById('blotter').getElementsByTagName('tbody')[0];
  var heartbeatRef = document.getElementById('heartbeat');
  var errorRef = document.getElementById('error');
  var header = document.getElementById('header');
  var editContainer = document.getElementById('editContainer');
  var editButtonBar = document.getElementById('editButtonBar');
  var pageContent = document.getElementById('pageContent');
  var configureMenu = document.getElementById('configure-menu');
  var attributeSpecs = {};
  
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

  function render(rowId, attributes)
  {
	var row = document.getElementById(rowId);
	
	if(row == null)
	{
	    // Insert a row in the table at the last row
	    row = tableRef.insertRow(-1);
	    row.id = rowId;
	    $(row).css("position", "relative");
//    	row.contentEditable = true;
//	    row.style.position = 'relative';
//	    row.style.display = 'grid';
	}
	
    for(i=0 ; i<header.cells.length ; i++)
    {
    	var cell;
    	
    	if(row.cells.length > i)
    		cell = row.cells[i];
    	else
    		cell = row.insertCell(-1);
    	
    	cell.classList.add(header.cells[i].id);
//    	cell.contentEditable = true;
    	
    	var attr = attributes[header.cells[i].innerHTML];
    	
    	if(attr == null)
    	{
    		cell.innerHTML = '';
    	}
    	else
    	{
    		renderCell(cell, attr);
    		delete attributes[header.cells[i].innerHTML];
    	}
    }
    
    for(var key in attributes)
    {
    	var attr = attributes[key];
    	var attrId = key.split(' ').join('_');
    	
    	if(attributeSpecs[attrId] == null)
    	{
    		attributeSpecs[attrId] = attr;
    	}
    	
    	var colId = 'COL_' + attrId;
    	var cell = document.createElement("th");

        header.appendChild(cell);
    	
    	cell.id = colId;
    	cell.innerHTML = key;
    	
    	var cell = row.insertCell(-1);
    	cell.classList.add(colId);
//    	cell.contentEditable = true;
    	
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
	  document.getElementById(id).classList.toggle("show");
  }
  
//  document.getElementById('blotter')
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
			if (event.target.closest('#blotter'))
				openEditPanel(event);
		}
	}
  }
  
  function saveComplete(data, textStatus, jqXHR)
  {
	  alert('data=' + data + ', textStatus=' + textStatus + ', jqXHR=' + jqXHR);
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
		editContainer.style.display='none'
		$(existingEditPanel).remove();
	}
  }
  
  function openEditPanel(event)
  {
	var trElement = event.target.closest('tr');
		
	$(editContainer).width(trElement.scrollWidth); 
	editContainer.style.display='block'
  

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
			for(i=0 ; i<editForm.children.length ; i++)
			{
				var editor = editForm.children[i];
				var id = editor.id.substring(5);
				var headerCell = document.getElementById(id);
				
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
	  
	  for(i=0 ; i<header.cells.length ; i++)
	  {
		var cell = trElement.cells[i];
		
		if(cell.style.display != "none")
		{
//				$(cell).height();
			var editor;
			
			var attrSpec = attributeSpecs[header.cells[i].id.substring(4)];
			
			editor = $("<textarea id=\"EDIT_" + header.cells[i].id + "\", name=\"" + attrSpec.id + "\">" + 
					(attrSpec == null || attrSpec.editText == null ? attrSpec.text : attrSpec.editText) + "</textarea>");
			editor.attrId = header.cells[i].id;
			editor.id = 'EDIT_' + header.cells[i].id;
			
			if(attrSpec == null || !attrSpec.editable)
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
			
	    	x += $(cell).outerWidth(true);
		}
	  }
  }