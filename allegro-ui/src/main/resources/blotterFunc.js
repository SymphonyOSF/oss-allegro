
  var tableRef = document.getElementById('blotter').getElementsByTagName('tbody')[0];
  var heartbeatRef = document.getElementById('heartbeat');
  var errorRef = document.getElementById('error');
  var header = document.getElementById('header');
  
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
	}
	
    for(i=0 ; i<header.cells.length ; i++)
    {
    	var cell;
    	
    	if(row.cells.length > i)
    		cell = row.cells[i];
    	else
    		cell = row.insertCell(-1);
    	
    	cell.classList.add(header.cells[i].id);
    	var attr = attributes[header.cells[i].innerHTML];
    	
    	if(attr == null)
    	{
    		cell.innerHTML = '';
    	}
    	else
    	{
    		if(Array.isArray(attr))
    		{
    			cell.innerHTML = '<span title="' + attr[1] + '">' + attr[0] + '</span>';
    		}
    		else
    		{
    			cell.innerHTML = attr;
    		}
    		delete attributes[header.cells[i].innerHTML];
    	}
    }
    
    for(var key in attributes)
    {
    	var attrId = key.split(' ').join('_');
    	var colId = 'COL_' + attrId;
    	var cell = document.createElement("th");

        header.appendChild(cell);
    	
    	cell.id = colId;
    	cell.innerHTML = key;
    	
    	var cell = row.insertCell(-1);
    	cell.classList.add(colId);
    	cell.innerHTML = attributes[key];
    	
    	var menu = document.getElementById('configure-menu-list');
    	
    	var checkBox = document.createElement("input");

    	checkBox.id = 'CHECK_' + attrId;
    	checkBox.attrId = attrId;
    	checkBox.type = 'checkbox';
    	checkBox.checked = true;
    	checkBox.onchange = function(){toggleCol(this.attrId);};
    	
    	var textNode = document.createTextNode(key);
    	
    	var li = document.createElement("li");
    	
    	li.append(checkBox);
    	li.append(textNode);
    	
    	menu.append(li);
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
  
  //Close the dropdown menu if the user clicks outside of it
  window.onclick = function(event) {
    if (!event.target.matches('.dropbtn')) {
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
  
//  function upsertException(storedObject, exception)
//  {
//    // Insert a row in the table at the last row
//    var newRow   = tableRef.insertRow(-1);
//    upsert(newRow, storedObject);
//    addCol(newRow, exception);
//  }