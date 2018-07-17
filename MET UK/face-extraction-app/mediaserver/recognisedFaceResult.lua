--
-- Filter to pass Result records with a Face Recognition match.
--
function pred(x) 
	return ( x.FaceRecognitionResult.identity.identifier ~= nil ) 
end
