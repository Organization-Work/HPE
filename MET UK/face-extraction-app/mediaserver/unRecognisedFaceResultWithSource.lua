--
-- Filter to pass ResultWithSource records with no Face Recognition match.
--
function pred(x) 
	return ( x.FaceRecognitionResultAndImage.identity.identifier == nil ) 
end
