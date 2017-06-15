<#if (post??)>

<#if (post.postLikesCount > 0)>
$("#postLikesCount_"+ ${post.id}).html('${I18n.getMessage("PostShow.likes")}: ${post.postLikesCount}');
<#else>
$("#postLikesCount_"+ ${post.id}).html("");
</#if>

$("#postLikes_anc_"+ ${post.id}).html('<a class="toolUiLink" href="#" onclick="removeUserPostlike(${post.id});return false;" rel="nofollow"><img class="imgicon" id="postLikes_img_${post.id}" src="${contextPath}/templates/${templateName}/images/like_yellow.png" alt="starred icon" title="${I18n.getMessage("PostShow.removeLike.title")}"/></a>');
</#if>


