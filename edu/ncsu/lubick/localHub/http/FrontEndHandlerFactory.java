package edu.ncsu.lubick.localHub.http;

import edu.ncsu.lubick.localHub.LocalHub;
import httpserver.HTTPException;
import httpserver.HTTPHandler;
import httpserver.HTTPHandlerFactory;
import httpserver.HTTPRequest;

public class FrontEndHandlerFactory extends HTTPHandlerFactory {

	private LocalHub localHub;

	public FrontEndHandlerFactory(LocalHub localHub) {
		this.localHub = localHub;
	}

	@Override
	public HTTPHandler determineHandler(String pathSegment, HTTPRequest request) throws HTTPException {

		//if(checkIfEquals(pathSegment, "clip", request))
		//	return new ClipHandler(request, server);
		//if(checkIfEquals(pathSegment, "developer", request))
		//	return new DeveloperHandler(request, server);
		//if(checkIfEquals(pathSegment, "node", request))
		//	return new NodeHandler(request, server);
		//if(checkIfEquals(pathSegment, "playback", request))
		//	return new PlaybackHandler(request, server);
		//if(checkIfEquals(pathSegment, "storyboard", request))
		//	return new StoryboardHandler(request, server);

		return new FileHandler(request, localHub);
	}

}
