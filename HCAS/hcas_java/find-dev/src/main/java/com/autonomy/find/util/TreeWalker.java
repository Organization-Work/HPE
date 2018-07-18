package com.autonomy.find.util;

import java.util.List;

public interface TreeWalker {
  Object select(List<String> path);
  TreeWalker update(List<String> path, Object data);
}
