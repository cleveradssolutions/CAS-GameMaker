if (async_load[? "type"] == "cas_init") 
{
    var success = async_load[? "success"]; // 1 or 0
    var error   = async_load[? "error"];   // error or undefined
    var country = async_load[? "country"]; // ISO-code country or undefined
    var consent_required = async_load[? "consent_required"]; // 1 or 0
    var consent_status   = async_load[? "consent_status"];   // int
	
	if (success)
	{
		CAS_LoadInterstitial();
		CAS_LoadRewarded();		
		//load Ads
	}
}