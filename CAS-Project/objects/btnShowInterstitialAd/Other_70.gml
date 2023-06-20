switch async_load[? "id"] {
	case CAS_ASYNC_RESPONSE_ON_INTERSTITIAL_COMPLETE:
		show_message_async("Interstitial complete")
	break
}
