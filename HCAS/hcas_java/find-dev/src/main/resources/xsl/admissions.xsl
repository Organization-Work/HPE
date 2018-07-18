<?xml version="1.0"?>
<!DOCTYPE stylesheet [
    <!ENTITY nbsp "&#160;">
]>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:common="http://exslt.org/common"
    xmlns:autn="http://schemas.autonomy.com/aci/"
    xmlns:docview="http://www.autonomy.com/find/docview"
    xmlns:saxon="http://saxon.sf.net/"
    extension-element-prefixes="common"
        >
    <xsl:output method="html" indent="no" encoding="UTF-8"/>
    <xsl:param name="docviewfile"/>
    
    <xsl:variable name="viewXml" select="document($docviewfile)"/>
    <xsl:variable name="docRoot" select="/"/>
    <xsl:variable name="hlEmbeddedClass" select="'HTAG'"/>
    <xsl:variable name="hlEmbeddedSrcClass" select="'HTAG'"/>
    
    <xsl:template match="/">
    <!-- <xsl:text disable-output-escaping='yes'>&lt;!DOCTYPE html&gt;</xsl:text> -->
        <html>
            <head>
            <meta http-equiv="X-UA-Compatible" content="IE=edge" />
             <script type="text/javascript" src="../resources/js/jquery-1.10.2.min.js"></script>
             <script type="text/javascript" src="../resources/js/jquery-migrate-1.2.1.js"></script>
              
              <script>
            <xsl:text>
                var Cssname =  localStorage.getItem("storageId");
		        var buildVersionConfig = Cssname;
		        function addCSSload(filename){ 
				 var head = document.getElementsByTagName('head')[0];
				
				 var style = document.createElement('link');
				 style.href = filename;
				 style.type = 'text/css';
				 style.rel = 'stylesheet';
				 head.appendChild(style);
				}
		      	
           		addCSSload('../css/bootstrap.min'+buildVersionConfig+'.css');
				addCSSload('../css/typeaheadjs'+buildVersionConfig+'.css');
				addCSSload('../css/bootstrap-datetimepicker.min'+buildVersionConfig+'.css');
				addCSSload('../css/admissions'+buildVersionConfig+'.css');
				addCSSload('../css/search.filters'+buildVersionConfig+'.css');
				addCSSload('../resources/css/cupertino/jquery-ui-1.8.23.custom.css');
				addCSSload('../resources/js/jstree/themes/default/style.min.css'); 				
				</xsl:text>
            </script>
             
             <script type="text/javascript">
        		<xsl:text>
       			
		      	var name =  localStorage.getItem("storageId");
		      
		      	var buildVersionConfig = name;
		      			      	
   		      	function addScriptload(filename){
				 var head = document.getElementsByTagName('head')[0];
				 var script = document.createElement('script');
				 script.src = filename;
				 script.type = 'text/javascript';
				 head.appendChild(script);
				}
				<!-- addScriptload('../scripts/jquery-1.10.2.min'+buildVersionConfig+'.js')-->
				<!-- addScriptload('../scripts/jquery-migrate-1.2.1'+buildVersionConfig+'.js');-->
                addScriptload('../scripts/bootstrap-datetimepicker.min'+buildVersionConfig+'.js');
                addScriptload('../resources/js/jquery-ui-1.8.23.custom.min.js' );
                addScriptload('../resources/js/json2-min.js' );
                addScriptload('../scripts/underscore'+buildVersionConfig+'.js');
                addScriptload('../scripts/resget'+buildVersionConfig+'.js');
                addScriptload('../scripts/search.events'+buildVersionConfig+'.js');
                addScriptload('../scripts/filter.contextmenu'+buildVersionConfig+'.js');
                addScriptload('../scripts/filter.events'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.filterform'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.filterItem'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.baseFilterItem'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.dateFilterItem'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.monthpartitionFilterItem'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.documentfolderFilterItem'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.matchFilterItem'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.numericFilterItem'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.filtergroup'+buildVersionConfig+'.js');
                addScriptload('../scripts/jquery.fieldlist-control'+buildVersionConfig+'.js');
                addScriptload('../scripts/bootstrap-2.3.2'+buildVersionConfig+'.js');
				addScriptload('../scripts/typeahead.bundle'+buildVersionConfig+'.js');
				addScriptload('../resources/js/jstree/jstree.js');
                </xsl:text>
        </script>  
           
                    <script type="text/javascript">
                    var Reference = "<xsl:value-of select="*/@reference"/>";
                    var Folders = <xsl:value-of select="*/@folders"/>;
                </script>
    			
    			<script type="application/json" id="tree-data">
                      <xsl:attribute name="conceptData">
 						<xsl:value-of select="*/@conceptData"/>	
 					 </xsl:attribute>			
 					 <xsl:attribute name="relData">
 						<xsl:value-of select="*/@relData"/>	
 					 </xsl:attribute>			
                      <xsl:attribute name="contextData">
 						<xsl:value-of select="*/@contextData"/>	
 					 </xsl:attribute>			
                      <xsl:attribute name="negData">
 						<xsl:value-of select="*/@negData"/>	
 					 </xsl:attribute>
 					 <xsl:attribute name="selectedNodes">
 						<xsl:value-of select="*/@selectedNodes"/>	
 					 </xsl:attribute>				
				</script>
				
				<!-- <script type="text/javascript" src="../scripts/admissions.js"></script> -->
				
				<script type="text/javascript">
        		<xsl:text>
       		
		      
		      	var admis =  localStorage.getItem("storageId");
		       
		      	var admis = admis;
		      	//alert(admis + " admis XSL FIle");
		      	
   		      	function addScriptload(filename){
				 var head = document.getElementsByTagName('head')[0];
				 var script = document.createElement('script');
				 script.src = filename;
				 script.type = 'text/javascript';
				 head.appendChild(script);
				// document.getElementsByTagName("head")[0].appendChild(script);
				}
                addScriptload('../scripts/admissions'+admis+'.js');
                
                //addScriptload('../scripts/admissions'+admis+'.js');
              </xsl:text>
        </script> 
			 </head>
            <body>
            	<span id="docviewmessage"><xsl:value-of select="*/@message"/></span>            	
                <xsl:choose>
                    <xsl:when test="./redacted">
                        <ul class="nav nav-tabs">
                            <li class="pull-right hide"><div id="result-nav"></div></li>
                        </ul>
                        <div id="restricted" class="alert-error full-height">
                            <h2 id="restricted-text" >Access to the details of this record is restricted.</h2>
                        </div>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:call-template name="docview"/>
                    </xsl:otherwise>
                </xsl:choose>
            </body>
        </html>
    </xsl:template>

    <xsl:template name="field-meta">
        <xsl:param name="field"/>
        <xsl:param name="value"/>

        <xsl:variable name="fieldValue">
            <xsl:choose>
                <xsl:when test="@outputEscape = false()">
                    <xsl:value-of select="$value" disable-output-escaping="yes"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$value"/>
                </xsl:otherwise>
            </xsl:choose>            
        </xsl:variable>

        <span>
            <xsl:attribute name="data-field"><xsl:value-of select="$field"/></xsl:attribute>
            
            <xsl:choose>
                <xsl:when test="@contentDb or @contentView ">
                    <a class="external-content-link">
                        <xsl:attribute name="href">
                            <xsl:value-of select="if (@contentDb) then concat('getLinkedDbContent.do?linkDb=', encode-for-uri(@contentDb)) else concat('getLinkedSearchViewContent.do?linkSearchView=', encode-for-uri(@contentView))" />
                            <xsl:text>&amp;linkId=</xsl:text><xsl:value-of select="encode-for-uri($fieldValue)"/>
                            <xsl:if test="@contentQParams">
                                <xsl:text>&amp;linkQParams=</xsl:text><xsl:value-of select="encode-for-uri(@contentQParams)"/>
                            </xsl:if>
                            <xsl:if test="@contentTemplate">
                                <xsl:text>&amp;linkTemplate=</xsl:text><xsl:value-of select="encode-for-uri(@contentTemplate)"/>
                            </xsl:if>
                        </xsl:attribute>
                        <xsl:value-of select="$fieldValue" disable-output-escaping="yes"/>
                    </a>
                 </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$fieldValue" disable-output-escaping="yes"/>
                </xsl:otherwise>
            </xsl:choose>
            
        </span>
    </xsl:template>

	
    <xsl:template name="agentstore-fields">
         
		 <div id="docview_rightpanel">
		 	<div id="popuploader" style="display:none"></div>
		    <input type="button" class="btn btn-default btn-mini" id="check" value="Check All" />
		    <div id="agentstore-fields" class="full-height">
				<h5>Document Tags</h5>
				<BR/>
				<form id="s" action="#">
				  Search Tags:<input type="search" id="q" />
				</form>
				<div id="foldertree">
				</div>
				<div id="navtree">
				</div>   
			</div>
		</div>
    </xsl:template>

    <xsl:template name="navbars">
        <xsl:param name="viewTabs"/>
        <ul class="nav nav-tabs">
            <xsl:for-each select="$viewTabs">
                <xsl:variable name="tabId" select="docview:getTabName(@name)"/>
                <xsl:variable name="sectionName" select="(./docview:section/@name)[1]"/> 
                <xsl:variable name="fieldName" select="(./docview:field/@name)[1]"/> 
                <xsl:variable name="countElementName" select="if($sectionName) then $sectionName else $fieldName"/>             
                <xsl:variable name="rootTagElement" select="string(root()/*/@docRoot)"/>
                <xsl:variable name="documentContext" select="$docRoot//saxon:evaluate($rootTagElement)"/>
                <xsl:variable name="countElement" select="$documentContext/saxon:evaluate($countElementName)"/>
                               
                <li>
                    <xsl:if test="@defautActive = true()">
                        <xsl:attribute name="class"><xsl:text>active</xsl:text></xsl:attribute>
                    </xsl:if>
                    <a data-toggle="tab">
                        <xsl:attribute name="href"><xsl:value-of select="concat('#', $tabId)"/></xsl:attribute>
                        <xsl:value-of select="@name"/>(<xsl:value-of select="count($countElement)"/>)
                    </a>
                </li>
            </xsl:for-each>
             <li class="pull-right hide"><div id="result-nav"></div></li>
            <li class="pull-right">
                <div class="hl-nav no-selection">
                    <a class="disabled hl-nav-link"  id="leftHLBtn" title="Previous HL"><i class="icon-chevron-left no-selection"/></a>
                    <div class="hl-nav-label">
                    <span id="currentHlLabel" class="hl-nav-text"></span>
                    <span id="hlLabel" class="hl-nav-text">&nbsp;</span>
                    <span id="totalHlLabel" class="hl-nav-text"></span>
                    </div>
                    <a class="disabled hl-nav-link" id="rightHLBtn" title="Next HL"><i class="icon-chevron-right no-selection"/></a>
                </div>
            </li>
        </ul>
    </xsl:template>
    
    <xsl:template name="viewtabRender">
        <div>
         
            <xsl:attribute name="id"><xsl:value-of select="docview:getTabName(@name)"/></xsl:attribute>
            <xsl:variable name="classes" select="if (@defaultActive = true()) then 'tab-pane active' else 'tab-pane'"/>
            <xsl:call-template name="renderStyleClass">
                <xsl:with-param name="classes" select="$classes"/>
            </xsl:call-template>
            
            <h2><xsl:value-of select="@name"/></h2>
            
            <xsl:variable name="rootTag" select="string(root()/*/@docRoot)"/>
            <xsl:apply-templates select="docview:field | docview:section | docview:header">
                <xsl:with-param name="docContext" select="$docRoot//saxon:evaluate($rootTag)"/>
                <xsl:with-param name="parentPath" select="''"/>
            </xsl:apply-templates>
        </div>
     </xsl:template>
	       

	 <xsl:template match="iframe">
		<xsl:param name="iframevalue"/>
		<div>
			<iframe height="300 px" width="90%" scrolling="true">
				<xsl:attribute name="srcdoc"><xsl:value-of select="$iframevalue"/></xsl:attribute>
			</iframe>
			</div>
	 </xsl:template>
    
    <xsl:template match="docview:field">
        <xsl:param name="docContext" required="yes"/>
        <xsl:param name="parentPath" required="yes"/>
        <xsl:variable name="fieldSelector" select="string(@name)"/>
        <xsl:variable name="dataField" select="if (@queryField) then @queryField else @name"></xsl:variable>
		<xsl:choose>
			<xsl:when test="@iframe">
				<div>
				<b><xsl:value-of select="concat(@label, ': ')"/></b>
					<iframe height="300 px" width="90%" scrolling="true">
						<xsl:attribute name="srcdoc"><xsl:value-of select="$docContext/saxon:evaluate($fieldSelector)"/></xsl:attribute>
					</iframe>
				</div>				
			</xsl:when>
			<xsl:otherwise>
				<div>
				<xsl:call-template name="renderStyleClass">
					<xsl:with-param name="classes" select="if (@hlEmbeddedText) then $hlEmbeddedClass else ''"/>
				</xsl:call-template>
				<b><xsl:value-of select="concat(@label, ': ')"/></b>
				<xsl:call-template name="field-meta">
					<xsl:with-param name="value" select="$docContext/saxon:evaluate($fieldSelector)"/>
					<xsl:with-param name="field" select="docview:fieldPath($parentPath, $dataField)"/>
				  </xsl:call-template>
				<xsl:if test="@contentDb or @contentView">
					<div class="external-content"/>
				</xsl:if>
			</div>   
			<xsl:if test="@hlEmbeddedText">
				<div>
					<xsl:call-template name="renderStyleClass">
						<xsl:with-param name="classes" select="$hlEmbeddedSrcClass"/>
					</xsl:call-template>
					<b><xsl:value-of select="concat(@label, ': ')"/></b>
					<xsl:call-template name="field-meta">
						<xsl:with-param name="value" select="$docContext/saxon:evaluate($dataField)"/>
						<xsl:with-param name="field" select="docview:fieldPath($parentPath, $dataField)"/>
					</xsl:call-template>
					<xsl:if test="@contentDb or @contentView">
						<div class="external-content"/>
					</xsl:if>
				</div>   
				
			</xsl:if>
			</xsl:otherwise>
		</xsl:choose>
			
		
    </xsl:template>

    <xsl:template match="docview:section">
        <xsl:param name="docContext" required="yes"/>
        <xsl:param name="parentPath" required="yes"/>		
        <xsl:variable name="sectionContext" select="."/>
        <xsl:variable name="sectionSelector" select="string(@name)"/>
        <xsl:variable name="sortField" select="string(@sort)"/>
        <xsl:variable name="sectionItems" select="docview:sfield | docview:section | docview:list | docview:header | docview:sectionheader"/>
        <xsl:variable name="hlEmbeddedText" select="@hlEmbeddedText"/>
        <xsl:for-each select="$docContext/saxon:evaluate($sectionSelector)">
        	  <xsl:sort select="*[name()=$sortField]"/> 
            <div>
                <xsl:call-template name="renderStyleClass">
                    <xsl:with-param name="context" select="$sectionContext"/>
                    <xsl:with-param name="classes" select="if ($hlEmbeddedText) then $hlEmbeddedClass else ''"/>
                </xsl:call-template>
                <xsl:apply-templates select="$sectionItems">
                    <xsl:with-param name="docContext" select="."/>   
                    <xsl:with-param name="parentPath" select="docview:fieldPath($parentPath, $sectionSelector)"/>   
                </xsl:apply-templates>
                
            </div>
        </xsl:for-each>
        
        <xsl:if test="$hlEmbeddedText">
            <xsl:variable name="srcSectionSelector" select="@queryField"/>
            <xsl:for-each select="$docContext/saxon:evaluate($srcSectionSelector)">
                <div>
                    <xsl:call-template name="renderStyleClass">
                        <xsl:with-param name="context" select="$sectionContext"/>
                        <xsl:with-param name="classes" select="$hlEmbeddedSrcClass"/>
                    </xsl:call-template>
                    <xsl:apply-templates select="$sectionItems">
                        <xsl:with-param name="docContext" select="."/>   
                        <xsl:with-param name="parentPath" select="docview:fieldPath($parentPath, $srcSectionSelector)"/>   
                    </xsl:apply-templates>
                    
                </div>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="docview:header | docview:sectionheader">
        <span><xsl:call-template name="renderStyleClass"/><xsl:value-of select="."/></span><br/>
    </xsl:template>

    <xsl:template match="docview:sfield">
        <xsl:param name="docContext" required="yes"/>
        <xsl:param name="parentPath" required="yes"/>
        <xsl:variable name="fieldSelector" select="string(@childName)"/>
        <xsl:variable name="dataField" select="if (@queryField) then @queryField else @childName"></xsl:variable>
        <xsl:variable name="childDataField">
            <xsl:choose>
                <xsl:when test="@childName = '.'">
                    <xsl:value-of select="if (@queryField) then $dataField else $parentPath"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="docview:fieldPath($parentPath, $dataField)"/>
                </xsl:otherwise>
            </xsl:choose>
            
        </xsl:variable>
		<xsl:choose>
			<xsl:when test="@iframe">
				<xsl:variable name="renderHTML" select="$docContext/saxon:evaluate($fieldSelector)"/>
				<xsl:if test="$renderHTML != ''">
				<div>
					<span>
						<xsl:attribute name="data-field"><xsl:value-of select="docview:fieldPath($parentPath, $dataField)"/></xsl:attribute>
						<b><xsl:value-of select="concat(@label, ': ')"/></b>
					<br/>
					<div class="docview_iframe_div" >
						<iframe class="docview_iframe_content" id="frame-{generate-id()}">
							<xsl:attribute name="content"><xsl:value-of select="$renderHTML" disable-output-escaping="yes"/></xsl:attribute>
						</iframe>
					<!-- <xsl:value-of select="$renderHTML" disable-output-escaping="yes"/>	  -->
	
					</div>			
					</span>
					<!--
					<div>
					<span>
							<xsl:attribute name="data-field"><xsl:value-of select="docview:fieldPath($parentPath, $dataField)"/></xsl:attribute>
							<iframe class="docview_iframe_control">
								<xsl:attribute name="srcdoc"><xsl:value-of select="$renderHTML" disable-output-escaping="yes"/></xsl:attribute>  
								<xsl:attribute name="id">ifr-id<xsl:number/></xsl:attribute>
							</iframe>
							<script type="text/javascript">
								var frid="fr";
								var framecontent="if"; 
							</script>
					</span>
					</div>
					-->
				</div>				
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<div>
					<xsl:call-template name="renderStyleClass">
						<xsl:with-param name="classes" select="if (@hlEmbeddedText) then $hlEmbeddedClass else ''"/>
					</xsl:call-template>
					<b><xsl:value-of select="concat(@label, ': ')"/></b>
					<xsl:call-template name="field-meta">
						<xsl:with-param name="value" select="$docContext/saxon:evaluate($fieldSelector)"/>
						<xsl:with-param name="field" select="$childDataField"/>
					</xsl:call-template>
					<xsl:if test="@contentDb or @contentView">
						<div class="external-content"/>
					</xsl:if>
				</div> 
			</xsl:otherwise>
		</xsl:choose>
        <!--  
        <xsl:if test="@hlEmbeddedText">
            <div>
                <xsl:call-template name="renderStyleClass">
                    <xsl:with-param name="classes" select="$hlEmbeddedSrcClass"/>
                </xsl:call-template>
                <b><xsl:value-of select="concat(@label, ': ')"/></b>
                <xsl:call-template name="field-meta">
                    <xsl:with-param name="value" select="$docContext/saxon:evaluate($dataField)"/>
                    <xsl:with-param name="field" select="$childDataField"/>
                </xsl:call-template>
                <xsl:if test="@contentDb or @contentView">
                    <div class="external-content"/>
                </xsl:if>
            </div>  
        </xsl:if>
        --> 
		    </xsl:template>

    <xsl:template match="docview:list">
        <xsl:param name="docContext" required="yes"/>
        <xsl:param name="parentPath" required="yes"/>
        <ul>
            <xsl:call-template name="renderStyleClass"/>
            <xsl:for-each select="docview:sfield">
                <li>
                    <xsl:apply-templates  select=".">
                        <xsl:with-param name="docContext" select="$docContext"/>   
                        <xsl:with-param name="parentPath" select="$parentPath"/>   
                    </xsl:apply-templates>                    
                </li>              
            </xsl:for-each>
        </ul>
    </xsl:template>
    
    <xsl:template name="docview">
        <xsl:variable name="viewTabs" select="$viewXml/*/docview:viewtab"/>
 
        <xsl:call-template name="navbars">
            <xsl:with-param name="viewTabs" select="$viewTabs"/>
        </xsl:call-template>
        <xsl:call-template name="agentstore-fields"/>
        
        <div class="tab-content full-height" id="record_container">
            <xsl:for-each select="$viewTabs">
                <xsl:call-template name="viewtabRender"/>
            </xsl:for-each>
            
            <div class="concepts-ontology hidden">
                <xsl:call-template name="concepts-tree"/>
            </div>
        </div>
            <!-- <div id="link-menu">
 			<ul class="link_menu">
        	<li class="hoverli" ><a id="OMIMGM_TERM_SEARCH" href="#" target="_blank">Search OMIM</a></li>
        	<li class="hoverli" ><a id="PUBMED_TERM_SEARCH" href="#" target="_blank">Search PubMed</a></li> 
        	</ul>
        	</div> -->
        
    </xsl:template>    
    
   <xsl:template name="concepts-tree">
       <xsl:variable name="conceptPlusRootNodes" select="//root-concept/@cid"/>
       <xsl:variable name="conceptPlusNodes" select="//CONCEPTS/SM_ISA"/>
       
       <xsl:for-each select="//CONCEPTS/SM_ISA">
           <xsl:copy-of select="."/>
       </xsl:for-each>
       
   </xsl:template>
    
    <xsl:template name="renderStyleClass">
        <xsl:param name="classes"/>
        <xsl:param name="context" select="."/>
        <xsl:variable name="styleClass" select="$context/@styleClass"/>

        <xsl:if test="$classes != '' or $styleClass != ''"> 
            <xsl:variable name="allClasses" select="if ($classes != '') then concat($classes, ' ', $styleClass) else $styleClass"/>
            <xsl:attribute name="class"><xsl:value-of select="replace(concat($classes, ' ', $styleClass), '^\s+|\s+$', '')"/></xsl:attribute>    
        </xsl:if>
    </xsl:template>
    
    
    <xsl:function name="docview:fieldPath">
        <xsl:param name="parent"/>
        <xsl:param name="child"/>
        <xsl:sequence select=" if ($parent) then concat($parent, '/', $child) else $child"/>
    </xsl:function>    

    <xsl:function name="docview:getTabName">
        <xsl:param name="viewTabName"/>
        <xsl:sequence select="concat('tabpage_', replace($viewTabName, '\s+', '_'))"/>
    </xsl:function>   
 
</xsl:stylesheet>