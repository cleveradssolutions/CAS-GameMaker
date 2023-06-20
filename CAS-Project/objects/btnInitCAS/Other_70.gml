switch async_load[? "id"] {
	case CAS_ASYNC_RESPONSE_INITIALIZED:
		show_message_async("CAS succesfully Initialized")
		break
	case CAS_ASYNC_RESPONSE_NOT_INITIALIZED:
		show_message_async("CAS not initialized with error: " + async_load[? "error"])
		break
}
