/**
 * search.util.js
 */
var Util = {};
Util.log = Config.DEBUG ? function() {
	return console.log(_.toArray(arguments));
} : _.identity
Util.setWindowHashNow = function(newhash) {
	window.location.hash = newhash;
};
Util.setWindowHash = _.debounce(Util.setWindowHashNow, Config.HASH_UPDATE_DELAY);
/**
 * Prevents an event from resulting in default behaviour.
 */
Util.preventEvent = function(e) {
	e.preventDefault();
	return false;
};
/**
 * Removes html tags and introduces/preserves some html-entities (so no
 * injection!)
 * 
 * Example: f('a &lt;b > g < &amp;gt; <i>c</i> d') => 'a &lt;b &gt; g &lt;
 * &amp;gt; c d' Should the output be used in html, it would be rendered as:
 * a <b > g < &gt; c d
 */
Util.cleanHtml = function (val) {
	var tmp = $('<div/>');
	return tmp.text(tmp.html(val).text()).html();
};

/**
 * from: http://exacttarget.github.io/fuelux/#preloader
 */
Util.preloader = function(e){
    var t,n;t=function(e){n.innerHTML=e;e++;if(e===9)e=1;setTimeout(function(){t(e)},125)};
    e=document.getElementById(e);n="preloader_"+(new Date).getTime();e.className+=" iefix";
    e.innerHTML='<span>0</span><b id="'+n+'"></b>'+e.innerHTML;n=document.getElementById(n);t(1)
};

Util.mapHeader = function(fields, ignoreSubGrouping) {
    var result = {};
    var header, entry;

    _.each(fields, function(value, key) {
        //value.name = key;
        header = value.parentCategory.name;
        if (!header) {
            header = '';
        }
        entry = result[header];
        if (!entry) {
            entry = {
                displayName: header,
                groups: {},
                values: [],
                ordinal: value.parentCategory.ordinal,
                sortByFieldOrdinal: value.parentCategory.sortByFieldOrdinal
            };
            result[header] = entry;
        }
        
        var addToCategory = true;
        var subgroup = value.parentGroup && value.parentGroup.name;
        if (subgroup && !ignoreSubGrouping) {
        	addToCategory = false;
        	if (!entry.groups[subgroup]) {
        		entry.groups[subgroup] = {
        			whenOperator: subgroup.whenOp,
        			values: []
        		};
        	}
        	entry.groups[subgroup].values.push(value);
        	if (!entry.groups[subgroup].whenOperator) {
        		entry.groups[subgroup].whenOperator = value.parentGroup.whenOp;
        	}
        } 
        if (addToCategory) {
        	entry.values.push(value);
        	if (!entry.whenOperator) {
        		entry.whenOperator = value.parentCategory.whenOp;
        	}
        }
    });

    result = _.map(result, function(entry){
    	if(entry.sortByFieldOrdinal) {
    		entry.values.sort(Util._sortFieldOrdinals);
    	} else {
    		entry.values.sort(Util._sortKeys);
    	}
        if (!_.isEmpty(entry.groups)) {
        	var sortedGroups = {};
        	
        	var sortedKeys = _.keys(entry.groups).sort(Util._sortOrdinals);
        	_.each(sortedKeys, function(key) {
        		var sortedGroupValues;
        		if(entry.sortByFieldOrdinal) {
        			sortedGroupValues = entry.groups[key].values.sort(Util._sortFieldOrdinals);
        		} else {
        			sortedGroupValues = entry.groups[key].values.sort(Util._sortKeys);
        		}
        		sortedGroups[key] = {
        			whenOperator: entry.groups[key].whenOperator,
        			values: sortedGroupValues
        		};
        		
        	});
        	
        	entry.groups = sortedGroups;
        }
        
        return entry;
    }).sort(Util._sortOrdinals);

    return result;
};

Util._sortKeys = function(a, b) {
    var nameA = a.displayName || a;
    var nameB = b.displayName || b;
    nameA = nameA.toLowerCase();
		nameB = nameB.toLowerCase();
		
    if (nameA === '') {
        return 1;
    } else if (nameB === '') {
        return -1;
    }

    if (nameA < nameB) {
        return -1;
    }
    if (nameA > nameB) {
        return 1;
    }

    return 0;
};

Util._sortOrdinals = function(a, b) {
    var diff = a.ordinal - b.ordinal;
    	
    return diff != 0 ? diff : Util._sortKeys(a, b);
}
Util._sortFieldOrdinals = function(a, b) {
    var diff = a.fieldOrdinal - b.fieldOrdinal;
    	
    return diff != 0 ? diff : Util._sortKeys(a, b);
}
