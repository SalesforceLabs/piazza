// make it safe to use console.log always
(function(b){function c(){}for(var d="assert,count,debug,dir,dirxml,error,exception,group,groupCollapsed,groupEnd,info,log,markTimeline,profile,profileEnd,time,timeEnd,trace,warn".split(","),a;a=d.pop();)b[a]=b[a]||c})(window.console=window.console||{});

(function($, window, undefined) {
    // 'format' function by stackoverflow user 'ianj'
    // http://stackoverflow.com/questions/1038746/equivalent-of-string-format-in-jquery/5341855#5341855
    // Code licensed under http://creativecommons.org/licenses/by-sa/3.0/
    $.format = function (str, col) {
        col = typeof col === 'object' ? col : Array.prototype.slice.call(arguments, 1);

        return str.replace(/\{\{|\}\}|\{(\w+)\}/g, function (m, n) {
            if (m == "{{") { return "{"; }
            if (m == "}}") { return "}"; }
            return col[n];
        });
    };

    /* highlight v3

    Highlights arbitrary terms.

    <http://johannburkard.de/blog/programming/javascript/highlight-javascript-text-higlighting-jquery-plugin.html>

    MIT license.

    Johann Burkard
    <http://johannburkard.de>
    <mailto:jb@eaio.com>
    */
    $.fn.highlight = function(pat) {
    	var innerHighlight = function (node, pat) {
    		var skip = 0;
    		if (node.nodeType == 3) {
    			var pos = node.data.toUpperCase().indexOf(pat);
    			if (pos >= 0) {
    				var spannode = document.createElement('span');
    				spannode.className = 'highlight';
    				var middlebit = node.splitText(pos);
    				var endbit = middlebit.splitText(pat.length);
    				var middleclone = middlebit.cloneNode(true);
    				spannode.appendChild(middleclone);
    				middlebit.parentNode.replaceChild(spannode, middlebit);
    				skip = 1;
    			}
    		}
    		else if (node.nodeType == 1 && node.childNodes && !/(script|style)/i.test(node.tagName)) {
    			for (var i = 0; i < node.childNodes.length; ++i) {
    				i += innerHighlight(node.childNodes[i], pat);
    			}
    		}
    		return skip;
    	}
    	return this.each(function() {
    		innerHighlight(this, pat.toUpperCase());
    	});
    };
    
    $.fn.removeHighlight = function() {
    	return this.find("span.highlight").each(function() {
    		this.parentNode.firstChild.nodeName;
    		with (this.parentNode) {
    			replaceChild(this.firstChild, this);
    			normalize();
    		}
    	}).end();
    };
    // END HIGHLIGHTING
    
    $.fn.refresh = function() {
        var page = $(this);
        var url = page.jqmData('url');
        if ($.trim(url) == ''){
            url = '/';
        }
        $.mobile.changePage(url, {transition: 'fade', reloadPage: true});
    };


    // Config
    $('.config').live('pagecreate', function(e) {
        var config = this;
        $(config).find('.deleteKeyword').live('vclick', function(evt) {
            if (evt) {
                evt.preventDefault();
            }

            // Get the parent li element, get the first anchor child
            // This is the keyword element, get text
            var elem = this;
            var keyword = $.trim($(elem).parents('li').first().select('a').first().text());
            $.ajax({url: '/keyword',
                data: {keyword: keyword, authenticityToken: window.csrfToken},
                type: 'DELETE',
                success: function() {
                    var ul = $(elem).parents('ul').first();
                    var li = $(elem).parents('li').remove();
                    $(ul).listview('refresh');
                }
            });
        });

        $(config).find('.createKeyword').live('vclick', function(evt) {
            if (evt) {
                evt.preventDefault();
            }

            var keyword = $.trim(window.prompt('Enter new keyword:')) || '';

            $.ajax({url: '/keyword',
                data: {keyword: keyword, authenticityToken: window.csrfToken},
                type: 'POST',
                success: function() {
                    var newRow = $('<li></li>')
                        .append($('<a href="#"></a>').text(keyword))
                        .append($('<a class="deleteKeyword" href="#">Delete</a>'));
                    $(config).find('.keywordList').append(newRow).listview('refresh');
                    $(config).find('.emptyMessage').hide();
                },
                error: function(xhr) {
                           alert(xhr.responseText);
                }
            });
        });
    });

    $('.respond').live('pageshow', function(e) {
        var respond = this;
        var msg = $(respond).find('.msg').first();
        msg.focus().click();
        msg.select();
    });
    
    $('.index').live('pagecreate', function(e) {
        var index = this;
        //highlighting keywords
        var keywords = $(index).find('.keywords').val().split(',');
        console.log(keywords);
        $.each(keywords, function(i, keyword) {
        	if (keyword) {
	        	console.log('highlight', keyword);
		        $(index).find('.tweet').highlight(keyword);
        	}
        });

        //highlight the conference hashtag as well
        var hashtag = $(index).find('.hashtag').val(); 
        if (hashtag) {
	        console.log('highlight', hashtag);
	        $(index).find('.tweet').highlight(hashtag);
        }

    });

    $('.waiting').live('pageshow', function() {
        $.mobile.showPageLoadingMsg();	
        var waiting = this;
        var success = function (geo) {
        	console.log('geoloc success, saving and redirecting');
            $.mobile.changePage('/location', {
                data: {lat: geo.coords.latitude, long_: geo.coords.longitude, authenticityToken: window.csrfToken},
                transition: 'slideup',
                type: 'POST'
            });
        };

        var error = function() {
        	console.log('geoloc failed redirecting to events');
            $.mobile.changePage('/location', {
            	type: 'DELETE',
            	data: {viewEvents: true, authenticityToken: window.csrfToken},
            	transition: 'slideup'
            });
        }
        
        if (navigator && navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(success, error, {enableHighAccuracy: true});
        } else {
            console.log('geolocation not awailable on browser.', $.browser);
            error();
        }
    });

    $('.event-list').live('pagecreate', function(e) {
    	$(this).find('li a').live('vclick', function() {
			$(this).find('form').submit();
	    });
    });
    
    // jQuery Mobile integration for Google Analytics
    // http://www.jongales.com/blog/2011/01/10/google-analytics-and-jquery-mobile/
    $('div').live('pageshow', function (e) {
        try {
            var pageTracker = _gat._getTracker("UA-1526821-5");
	        var url = $(this).jqmData('url');
            console.log('Opened url', url);
            pageTracker._trackPageview(url);
        } catch(err) { }
    });
    
    $('.person-show').live('pagecreate', function() {
        var person = this;
        //highlighting keywords
        var keywords = $(person).find('.keywords').val().split(',');
        console.log(keywords);
        $.each(keywords, function(i, keyword) {
            if (keyword) {
                console.log('highlight', keyword);
                $(person).find('.relevantTweet').highlight(keyword);
            }
        });

        //highlight the conference hashtag as well
        var hashtag = $(person).find('.hashtag').val(); 
        if (hashtag) {
            console.log('highlight', hashtag);
            $(person).find('.relevantTweet').highlight(hashtag);
        }
    });
})(jQuery, window);
