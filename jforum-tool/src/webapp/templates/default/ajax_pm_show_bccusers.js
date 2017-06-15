<#if (users??)>
var bccUsernameSel = document.getElementById("bccUsername");
<#list users as user>

var name = "${user.lastName?default("")?js_string}, ${user.firstName?default("")?js_string}";

var option = document.createElement("option");
option.value = ${user.id};
option.innerHTML = name;
bccUsernameSel.appendChild(option);

</#list>
</#if>
