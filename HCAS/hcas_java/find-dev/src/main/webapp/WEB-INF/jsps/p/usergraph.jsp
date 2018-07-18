<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="X-UA-Compatible" content="IE=9">
    <title>Usergraph</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/cupertino/jquery-ui-1.8.23.custom.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/reader.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/tweet.css" type="text/css">
    <link rel="stylesheet" href="<%=request.getContextPath()%>/resources/css/usergraph.css" type="text/css">
</head>
<body>
    <div id="paper"></div>
    <form id="controls">
        <div class="magnifier"></div>
        <!-- Using a zero-width space in the emptytext so people can search for the literal string 'search' -->
        <input id="query" name="query" size="10" title="Query" data-emptytext="s&#8203;earch" value="s&#8203;earch">
        <input id="searchbtn" type="submit" value="Go">
        <span id="load-indicator"></span>
    </form>
    <div id="peopleLabel">
        <div class="label">People</div>
    </div>
    <div id="relationshipsLabel">
        <div class="label">Relationships</div>
        <div id="slidercontainer" style="margin-top: 10px">
            <div class="sliderside">Weak Links</div>
            <div id="mintermslider" style="width:180px; margin-left: 10px; margin-right: 10px; display: inline-block;;"></div>
            <div class="sliderside">Strong Links</div>
        </div>
    </div>
    <div id="themesLabel">
        <div class="label">Themes</div>
        <div id="aqgcontainer">
        </div>
        <div id="samplescontainer">
            <div class="label">Samples</div>
            <div id="samples"></div>
        </div>
    </div>

    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/json2-min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/underscore-min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/polyfill.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery-ui-1.8.23.custom.min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/jquery.mousewheel.min.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/raphael.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/d3.patch.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/d3.v2.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/profileDocTpl.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/reader.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath()%>/resources/js/Autn/usergraph_canvas.js"></script>
    <script>
        $(function(){
            var maxUsersParam = /[?&]maxUsers=(\d+)(&|$)/i.exec(window.location.search),
                maxUsers = maxUsersParam ? Number(maxUsersParam[1]) : 300,
                mintermParam = /[?&](?:minlinks|minterm)=(\d+)(&|$)/i.exec(window.location.search),
                minLinks = mintermParam ? Number(mintermParam[1]) || 0 : 5,
                profileTermsParam = /[?&]profileterms=(\d+)(&|$)/i.exec(window.location.search),
                profileTerms = profileTermsParam ? Number(profileTermsParam[1]) || 0 : 200,
                suggestMinScoreParam = /[?&]suggestminscore=(\d+(\.\d*)?)(&|$)/i.exec(window.location.search),
                suggestMinScore = suggestMinScoreParam ? Number(suggestMinScoreParam[1]) || 0 : undefined,
                usersMinScoreParam = /[?&]usersminscore=(\d+(\.\d*)?)(&|$)/i.exec(window.location.search),
                usersMinScore = usersMinScoreParam ? Number(usersMinScoreParam[1]) || 0 : 20,
                showTermText = /[?&]termtext=true(&|$)/i.test(window.location.search),
                termWeights = !/[?&]termWeights=false(&|$)/i.test(window.location.search);

            var threadPopups = {};

            var docTpl = Autn.profileDocTpl;

            var colors = d3.scale.category20();

            var engine = 1;

            function tokenReplace(str, startTag, endTag) {
                return str && str.replace(/&lt;(&#x2F;)?autn:highlight&gt;/g, function(str, end){
                    return end ? endTag : startTag;
                });
            }

            function fetchProfiles(docIds){
                return $.ajax('termsForProfiles.json', {
                    contentType: 'application/json',
                    data: JSON.stringify({ engine: engine, docIds: docIds, maxTerms: profileTerms, expand: showTermText }),
                    type: 'POST'
                });
            }

            Autn.usergraph({
                minLinks: minLinks,
                fetchProfiles: fetchProfiles,
                showTermText: showTermText,
                termWeights: termWeights,
                fetchQuerySummary: function(terms) {
                    return $.ajax('querysummary.json', {
                        data: { engine: engine, query: terms.join(' '), maxResults: 200, minScore: suggestMinScore},
                        type: 'POST'
                    });
                },
                fetchSuggestions: function(terms) {
                    return $.ajax('query.json', {
                        data: { engine: engine, query: terms.join(' '), matchAllTerms: false, pageSize: 12, pageNum: 0,
                            minScore: suggestMinScore, totalResults: false},
                        traditional: true,
                        type: 'POST'
                    });
                },
                showNodeDocs: function(node, nodegroupId, evt, terms){
                    if (terms && !evt.ctrlKey) {
                        // we know it's a linkmode=cluster query, fetch the terms
                        Autn.Reader.query([terms.join(' ')], {
                            matchAllTerms: false,
                            minScore: suggestMinScore,
                            engine: engine,
                            title: 'Results relevant to ' + node.name + '\'s community',
                            url: 'query.json'
                        });
                        return;
                    }

                    var uniqueRef = node.name;
                    var popup = threadPopups[uniqueRef];

                    if (popup) {
                        if (popup !== true) {
                            popup.dialog('moveToTop');
                        }

                        return;
                    }

                    $.ajax('userSuggestions.json', {
                        data: {user: node.name, minScore: suggestMinScore},
                        success: function(json){
                            if (!threadPopups[uniqueRef]) {
                                var withDocs = 0;
                                var markup = '<div class="autndoc-popup">';

                                if (!json.profiles.length) {
                                    markup += _.escape(json.user) + ' has no profiles';
                                }
                                else {
                                    _.each(json.profiles, function(profile, idx){
                                        if (profile.docs.length) {
                                            var visibleIndex = ++withDocs;
                                            var color = colors(visibleIndex);
                                            var startTag = '<span class="autn-highlight" style="color: '+color+'">';
                                            var endTag = '</span>';
                                            markup += '<div class="profile" style="border-color: '+color+'"><div><div class="profile-header"><span class="legendpad" style="background-color: '+color+'">&nbsp;</span>Profile #'+(visibleIndex)+'</div><div class="profile-body">';
                                            _.each(profile.docs, function(doc, idx){
                                                markup += tokenReplace(docTpl({doc: doc, idx: idx}), startTag, endTag);
                                            });
                                            markup += '</div></div></div>';
                                        }
                                    });
                                }

                                markup += '</div>';

                                threadPopups[uniqueRef] = $(markup).dialog({
                                    maxHeight: 550,
                                    title: _.escape('Suggested results from ' + node.name + '\'s profiles'),
                                    width: 900,
                                    close: function() {
                                        delete threadPopups[uniqueRef];
                                    }
                                }).on('click', 'div.profiledoc-header,div.profile-header', function(evt) {
//                                    $(this).closest('.profiledoc').find('.profiledoc-body').toggleClass('profile-collapsed');
                                    if ($(evt.target).closest('a').length) {
                                        return;
                                    }

                                    var el = $(this);
                                    var bodyEl = el.parent().find(el.hasClass('profile-header') ? '.profile-body' : '.profiledoc-body');
                                    if (bodyEl.hasClass('profile-collapsed')) {
                                        bodyEl.switchClass('profile-collapsed', '');
                                    }
                                    else {
                                        bodyEl.switchClass('', 'profile-collapsed');
                                    }
                                    return false;
                                }).find('a').blur();
                            }
                        }
                    });
                },
                searchCallback: function(onSuccess, query, minDate){
                    // does a mindate parameter even make sense here?
                    // more refactoring necessary to make those controls optional
                    return $.ajax('usergraph.json', {
                        success: onSuccess,
                        data: {
                            maxUsers: maxUsers,
                            minScore: usersMinScore,
                            query: query
                        }
                    });
                }
            });
        });
    </script>
</body>
</html>

