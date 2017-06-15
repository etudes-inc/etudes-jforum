function verifyCheckedPMs()
{
	//var f = document.privmsg_list.id;
	var f = document.getElementsByName("id");
	
	if (f == undefined)	 {
		return false;
	}
	
	if (f.length == undefined)	 {
		if (f.checked) {
			return true;
		}
	}

	for (var i = 0; i < f.length; i++) {
		if (f[i].checked) {
			return true;
		}
	}
	
	alert("${I18n.getMessage("PrivateMessage.SelectTopics")}");
	return false;
}

function verifyCheckedPMDrafts()
{
	//var f = document.privmsg_list.id;
	var f = document.getElementsByName("id");
	
	if (f == undefined)	 {
		return false;
	}
	
	if (f.length == undefined)	 {
		if (f.checked) {
			return true;
		}
	}

	for (var i = 0; i < f.length; i++) {
		if (f[i].checked) {
			return true;
		}
	}
	
	alert("${I18n.getMessage("PrivateMessage.SelectDrafts")}");
	return false;
}

function flagPMs(mode)
{
	if (verifyCheckedPMs())
	{
		var ele = document.getElementById('action');
		ele.value = mode;
		document.privmsg_list.submit();
	}
}

function deletePMs()
{
	if (verifyCheckedPMs() && confirmPMDelete())
	{
		var ele = document.getElementById('action');
		ele.value = 'delete';
		document.privmsg_list.submit();
	}
}

function deletePMDrafts()
{
	if (verifyCheckedPMDrafts() && confirmPMDraftsDelete())
	{
		var ele = document.getElementById('action');
		ele.value = 'delete';
		document.privmsg_list.submit();
	}
}

function confirmPMDelete()
{
	if (confirm("${I18n.getMessage("PrivateMessage.ConfirmDelete")}")) 
	{
		return true;
	}
	
	return false;
}

function confirmPMDraftsDelete()
{
	if (confirm("${I18n.getMessage("PrivateMessage.ConfirmDraftsDelete")}")) 
	{
		return true;
	}
	
	return false;
}

function markReadPMs()
{
	if (verifyCheckedPMs())
	{
		var ele = document.getElementById('action');
		ele.value = 'markRead';
		document.privmsg_list.submit();
	}
}

function moveToFolder()
{
	if (verifyCheckedPMs())
	{
		var ele = document.getElementById('action');
		ele.value = 'moveMessages';
		document.privmsg_list.submit();
	}
	
}

function moveToSelectFolder(fid)
{
	if (verifyCheckedPMs())
	{
		var ele = document.getElementById('action');
		ele.value = 'moveToFolder';
		
		var eleTo = document.getElementById('to_folder');
		eleTo.value = fid;
		
		document.privmsg_list.submit();
	}
	
}

function deletePMFolders()
{
	if (verifyCheckedPMFolders())
	{
		var ele = document.getElementById('action');
		ele.value = 'deleteFolders';
		document.privmsg_fol_list.submit();
	}
}

function verifyCheckedPMFolders()
{
	var f = document.getElementsByName("id");
	
	if (f == undefined)	 
	{
		return false;
	}
	
	if (f.length == undefined)	 
	{
		if (f.checked) 
		{
			return true;
		}
	}

	for (var i = 0; i < f.length; i++) 
	{
		if (f[i].checked) 
		{
			return true;
		}
	}
	
	alert("${I18n.getMessage("PrivateMessage.SelectFolders")}");
	
	return false;
}