$( document ).on( "pagecreate", "#Account",function(){
			
	$( document ).on( "swipeleft swiperight", "#Account", function( e ) {
	 // We check if there is no open panel on the page because otherwise
	 // a swipe to close the left panel would also open the right panel (and v.v.).
	 // We do this by checking the data that the framework stores on the page element (panel: open).
		if ( $( ".ui-page-active" ).jqmData( "panel" ) !== "open" ) {
			if ( e.type === "swipeleft" ) {
				$( "#Account-right-panel" ).panel( "open" );
			} else if ( e.type === "swiperight" ) {
				$( "#Account-left-panel" ).panel( "open" );
			}
		}
	 });
});

$( document ).on( "pagecreate", "#Register",function(){
	
	$( document ).on( "swipeleft swiperight", "#Register", function( e ) {
	 // We check if there is no open panel on the page because otherwise
	 // a swipe to close the left panel would also open the right panel (and v.v.).
	 // We do this by checking the data that the framework stores on the page element (panel: open).
		if ( $( ".ui-page-active" ).jqmData( "panel" ) !== "open" ) {
			if ( e.type === "swipeleft" ) {
				$( "#Register-right-panel" ).panel( "open" );
			} else if ( e.type === "swiperight" ) {
				$( "#Register-left-panel" ).panel( "open" );
			}
		}
	 });
});