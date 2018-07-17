--
-- Filter to pass ResultWithSource records with a Face Recognition match.
--
function pred(x) 
	return ( x.FaceRecognitionResultAndImage.identity.identifier ~= nil ) 
end
