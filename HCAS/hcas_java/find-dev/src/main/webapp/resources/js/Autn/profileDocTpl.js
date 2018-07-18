if (typeof Autn === 'undefined') {
    Autn = {};
}

Autn.profileDocTpl = _.template(
    '<div class="profiledoc"><div class="profiledoc-header"><div class="profiledoc-title"><%-ctx.doc.title%><%if(/^https?:/.exec(ctx.doc.ref)){%><a target="_blank" href="<%-ctx.doc.ref%>"><img class="profiledoc-link" src="resources/images/link.png"></a><%}%></div></div><div class="profiledoc-body <%=ctx.idx >=2 ? "profile-collapsed" : ""%>"><%-ctx.doc.summary%></div></div>',
    undefined, {variable:'ctx'});