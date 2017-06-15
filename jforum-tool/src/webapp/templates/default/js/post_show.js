function handleBbCode(evt)
{
	var e = evt || window.event;
	var thisKey = e.which || e.keyCode;

	var ch = String.fromCharCode(thisKey).toLowerCase();
	
	if (e.altKey && ch == "b") {
		bbstyle(0);
	}
	else if (e.altKey && ch == "i") {
		bbstyle(2);
	}
	else if (e.altKey && ch == "u") {
		bbstyle(4);
	}
	else if (e.altKey && ch == "q") {
		bbstyle(6);
	}
	else if (e.altKey && ch == "c") {
		bbstyle(8);
	}
	else if (e.altKey && ch == "l") {
		bbstyle(10);
	}
	else if (e.altKey && ch == "p") {
		bbstyle(12);
	}
	else if (e.altKey && ch == "w") {
		bbstyle(14);
	}
}

function enterText(field)
{
	storeCaret(field);
	document.onkeydown = handleBbCode;
}

function leaveText()
{
	document.onkeydown = null;
}

//Depends on jquery.js
function addUserPostlike(postId)
{
	var p = { 
		post_id:postId
	};
	
	$.ajax({
		type:"POST",
		url:CONTEXTPATH + "/jforum" + SERVLET_EXTENSION + "?module=ajax&action=addUserPostLike",
		data:p,
		dataType:"script",
		global:false
	});
}

function removeUserPostlike(postId)
{
	var p = { 
			post_id:postId
		};
		
		$.ajax({
			type:"POST",
			url:CONTEXTPATH + "/jforum" + SERVLET_EXTENSION + "?module=ajax&action=removeUserPostLike",
			data:p,
			dataType:"script",
			global:false
		});
}