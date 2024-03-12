/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj;

/**
 * Class that declares the Python methods that can be used then in the script for several tasks
 * such as getting the edges from the segmentation mask and many others
 * 
 * @author Carlos Garcia Lopez de Haro
 */
public class PythonMethods {

	/**
	 * String containing the Python methods needed to find the border of binary masks
	 */
	protected static String TRACE_EDGES = ""
			+ "def is_edge_pixel(image, cx,cy):" + System.lineSeparator()
			+ "    # assuming image[cy,cx] != 0" + System.lineSeparator()
			+ "    h,w = image.shape" + System.lineSeparator()
			+ "    if cx < 0 or cx >= w or cy < 0 or cy >= h:" + System.lineSeparator()
			+ "        return False" + System.lineSeparator()
			+ "    # NB: cx,cy are valid coords" + System.lineSeparator()
			+ "    return cy == 0 or image[cy-1,cx] == 0 or cx == 0 or image[cy,cx-1] == 0 or cx == w-1 or image[cy,cx+1] == 0 or cy == h-1 or image[cy+1,cx] == 0" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "def find_contour_neighbors(image, cx,cy, last_forward_dir):" + System.lineSeparator()
			+ "    ccw_dir = [8,9,6,3,2,1,4,7] # \"numpad directions\"" + System.lineSeparator()
			+ "    # so, directions are coded, and numbers 1-4,6-9 are used," + System.lineSeparator()
			+ "    # let's use them as indices to the arrays below:" + System.lineSeparator()
			+ "    dir_idx = [0, 5,4,3, 6,0,2, 7,0,1]             # where is the code in the ccw_dir" + System.lineSeparator()
			+ "    counter_shifted_dir = [0, 6,9,8, 3,0,7, 2,1,4] # opposite code plus one in ccw" + System.lineSeparator()
			+ "    dir_dx = [0, -1,0,+1, -1,0,+1, -1,0,1]         # code translated to shift in x-axis" + System.lineSeparator()
			+ "    dir_dy = [0, +1,+1,+1, 0,0,0,  -1,-1,-1]       # code translated to shift in y-axis" + System.lineSeparator()
			+ "    #" + System.lineSeparator()
			+ "    # find first bro in the ccw direction starting from the direction" + System.lineSeparator()
			+ "    # from which we have come to this position" + System.lineSeparator()
			+ "    test_dir = counter_shifted_dir[last_forward_dir]" + System.lineSeparator()
			+ "    #print(f\"starting pos [{cx},{cy}], forward dir code {last_forward_dir}, thus examine code {test_dir}\")" + System.lineSeparator()
			+ "    nx = cx+dir_dx[test_dir]" + System.lineSeparator()
			+ "    ny = cy+dir_dy[test_dir]" + System.lineSeparator()
			+ "    while not (is_edge_pixel(image,nx,ny) and image[ny,nx] != 0):" + System.lineSeparator()
			+ "        #print(f\"  not contour at [{nx},{ny}], examined dir code {test_dir}\")" + System.lineSeparator()
			+ "        test_dir = ccw_dir[ (dir_idx[test_dir]+1)%8 ]" + System.lineSeparator()
			+ "        nx = cx+dir_dx[test_dir]" + System.lineSeparator()
			+ "        ny = cy+dir_dy[test_dir]" + System.lineSeparator()
			+ "    #print(f\"  happy at [{nx},{ny}], examined dir code {test_dir}\")" + System.lineSeparator()
			+ "    return nx,ny,test_dir" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "def trace_contour(image, max_iters, offset_x = 0, offset_y = 0):" + System.lineSeparator()
			+ "    sy = 0" + System.lineSeparator()
			+ "    sx = np.where(image[0] != 0)[0][0]" + System.lineSeparator()
			+ "    last_forward_dir = 1" + System.lineSeparator()
			+ "    #" + System.lineSeparator()
			+ "    x_coords = [int(sx+offset_x)]" + System.lineSeparator()
			+ "    y_coords = [int(sy+offset_y)]" + System.lineSeparator()
			+ "    x,y,last_forward_dir = find_contour_neighbors(image, sx,sy,last_forward_dir)" + System.lineSeparator()
			+ "    cnt = 1" + System.lineSeparator()
			+ "    while not (x == sx and y == sy) and cnt < max_iters:" + System.lineSeparator()
			+ "        x_coords.append(int(x+offset_x))" + System.lineSeparator()
			+ "        y_coords.append(int(y+offset_y))" + System.lineSeparator()
			+ "        x,y,last_forward_dir = find_contour_neighbors(image, x,y,last_forward_dir)" + System.lineSeparator()
			+ "        cnt += 1" + System.lineSeparator()
			+ "    #" + System.lineSeparator()
			+ "    return x_coords,y_coords" + System.lineSeparator()
			+ "" + System.lineSeparator()
			+ "def get_polygons_from_binary_mask(sam_result, at_least_of_this_size = 3, only_biggest=False):" + System.lineSeparator()
			// TODO the line below causes some overhead because of regioprops, mayube initialize in encoding
			+ "    labels = measure.regionprops( measure.label(sam_result,connectivity=1) )" + System.lineSeparator()
			+ "    x_contours = []" + System.lineSeparator()
			+ "    y_contours = []" + System.lineSeparator()
			+ "    sizes = []" + System.lineSeparator()
			+ "    for obj in labels:" + System.lineSeparator()
			+ "        if obj.num_pixels >= at_least_of_this_size:" + System.lineSeparator()
			+ "            x_coords,y_coords = trace_contour(obj.image, obj.num_pixels, obj.bbox[1],obj.bbox[0])" + System.lineSeparator()
			+ "            x_contours.append(x_coords)" + System.lineSeparator()
			+ "            y_contours.append(y_coords)" + System.lineSeparator()
			+ "            sizes.append(obj.num_pixels)" + System.lineSeparator()
			+ "    if only_biggest:" + System.lineSeparator()
			+ "        max_size_pos = np.array(sizes).argmax()" + System.lineSeparator()
			+ "        x_contours = [x_contours[max_size_pos]]" + System.lineSeparator()
			+ "        y_contours = [y_contours[max_size_pos]]" + System.lineSeparator()
			+ "    return x_contours,y_contours" + System.lineSeparator()
			+ "globals()['is_edge_pixel'] = is_edge_pixel" + System.lineSeparator()
			+ "globals()['find_contour_neighbors'] = find_contour_neighbors" +  System.lineSeparator()
			+ "globals()['trace_contour'] = trace_contour" +  System.lineSeparator()
			+ "globals()['get_polygons_from_binary_mask'] = get_polygons_from_binary_mask" +  System.lineSeparator();
}
