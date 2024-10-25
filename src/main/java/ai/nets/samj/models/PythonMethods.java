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
package ai.nets.samj.models;

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
			+ "def get_polygons_from_binary_mask(sam_result, at_least_of_this_size = 6, only_biggest=False):" + System.lineSeparator()
			+ "    labels = measure.regionprops( measure.label(sam_result > 0,connectivity=1) )" + System.lineSeparator()
			+ "    x_contours = []" + System.lineSeparator()
			+ "    y_contours = []" + System.lineSeparator()
			+ "    rles = []" + System.lineSeparator()
			+ "    sizes = []" + System.lineSeparator()
			+ "    for obj in labels:" + System.lineSeparator()
			+ "        if obj.num_pixels >= at_least_of_this_size:" + System.lineSeparator()
			+ "            x_coords,y_coords = trace_contour(obj.image, obj.num_pixels, obj.bbox[1],obj.bbox[0])" + System.lineSeparator()
			+ "            rles = encode_rle(obj.image)" + System.lineSeparator()
			+ "            for i in range(0, len(lst), 2):" + System.lineSeparator()
			+ "              rles[i] += sam_result.shape[1] * (obj.bbox[0] + i) +  obj.bbox[1]:" + System.lineSeparator()
			+ "            rles.append(encode_rle(obj.image))" + System.lineSeparator()
			+ "            x_contours.append(x_coords)" + System.lineSeparator()
			+ "            y_contours.append(y_coords)" + System.lineSeparator()
			+ "            sizes.append(obj.num_pixels)" + System.lineSeparator()
			+ "    if only_biggest:" + System.lineSeparator()
			+ "        max_size_pos = np.array(sizes).argmax()" + System.lineSeparator()
			+ "        x_contours = [x_contours[max_size_pos]]" + System.lineSeparator()
			+ "        y_contours = [y_contours[max_size_pos]]" + System.lineSeparator()
			+ "        rles = [rles[max_size_pos]]" + System.lineSeparator()
			+ "    return x_contours, y_contours, rles" + System.lineSeparator()
			+ "globals()['is_edge_pixel'] = is_edge_pixel" + System.lineSeparator()
			+ "globals()['find_contour_neighbors'] = find_contour_neighbors" +  System.lineSeparator()
			+ "globals()['trace_contour'] = trace_contour" +  System.lineSeparator()
			+ "globals()['get_polygons_from_binary_mask'] = get_polygons_from_binary_mask" +  System.lineSeparator();
	
	/**
	 * String containing a Python method to encode binary masks into a compressed object using the
	 * Run-Length Encoding (RLE) algorithm
	 */
	protected static String RLE_METHOD = ""
			+ "def encode_rle(mask):" +  System.lineSeparator()
			+ "    \"\"\"" +  System.lineSeparator()
			+ "    Encode a binary mask using Run-Length Encoding (RLE)." +  System.lineSeparator()
			+ "    " +  System.lineSeparator()
			+ "    Args:" +  System.lineSeparator()
			+ "        mask: A 2D binary array (numpy array) where 1 represents the object" +  System.lineSeparator()
			+ "             and 0 represents the background" +  System.lineSeparator()
			+ "    " +  System.lineSeparator()
			+ "    Returns:" +  System.lineSeparator()
			+ "        List[int]: RLE encoding in the format [start1, length1, start2, length2, ...]" +  System.lineSeparator()
			+ "                  where start positions are 0-based" +  System.lineSeparator()
			+ "    \"\"\"" +  System.lineSeparator()
			+ "    if isinstance(mask, list):" +  System.lineSeparator()
			+ "        mask = np.array(mask)" +  System.lineSeparator()
			+ "    " +  System.lineSeparator()
			+ "    # Flatten the mask in row-major order" +  System.lineSeparator()
			+ "    binary = mask.flatten()" +  System.lineSeparator()
			+ "    " +  System.lineSeparator()
			+ "    # Find positions where values change" +  System.lineSeparator()
			+ "    transitions = np.where(binary[1:] != binary[:-1])[0] + 1" +  System.lineSeparator()
			+ "    transitions = np.concatenate(([0], transitions, [len(binary)]))" +  System.lineSeparator()
			+ "    " +  System.lineSeparator()
			+ "    # Initialize result" +  System.lineSeparator()
			+ "    rle = []" +  System.lineSeparator()
			+ "    " +  System.lineSeparator()
			+ "    # Process each run" +  System.lineSeparator()
			+ "    for i in range(len(transitions) - 1):" +  System.lineSeparator()
			+ "        start = transitions[i]" +  System.lineSeparator()
			+ "        length = transitions[i + 1] - transitions[i]" +  System.lineSeparator()
			+ "        " +  System.lineSeparator()
			+ "        # Only encode runs of 1s" +  System.lineSeparator()
			+ "        if binary[start] == 1:" +  System.lineSeparator()
			+ "            rle.extend([int(start), int(length)])" +  System.lineSeparator()
			+ "    " +  System.lineSeparator()
			+ "    return rle" +  System.lineSeparator()
			+ "globals()['encode_rle'] = encode_rle" + System.lineSeparator();
	
	
	protected static String SAM_EVERYTHING = ""
			+ "def calculate_pairs(masks):\n"
			+ "    added_masks = masks.sum(2)\n"
			+ "    inds = np.where(added_masks > 1)\n"
			+ "    pairs = np.zeros((0, 2))\n"
			+ "    for ii in range(inds.shape[0]):\n"
			+ "        overlapping = np.where(masks[inds[0][ii], inds[1][ii]])[0]\n"
			+ "        for i in range(overlapping[0].shape[0]):\n"
			+ "            for j in range(i + 1, overlapping[0].shape[0]):\n"
			+ "                pp = np.unique(np.array([overlapping[i], overlapping[j]])).reshape(-1, 2)\n"
			+ "                matches = np.all(pairs == pp, axis=1)\n"
			+ "                if not matches.any():\n"
			+ "                    pairs = np.concatenate((pairs, pp), axis=0)\n"
			+ "    return pairs\n"
			+ "\n"
			+ "def sam_everything(point_list, return_all=False):\n"
			+ "    masks = np.zeros((input_h, input_w, 0), dtype='uint8')\n"
			+ "    scores = []\n"
			+ "    for ii, pp in enumerate(point_list):\n"
			+ "        input_points = np.array(pp).reshape(1, 2)\n"
			+ "        input_points = torch.reshape(torch.tensor(input_points), [1, 1, -1, 2])\n"
			+ "        input_label = np.array([0])\n"
			+ "        input_label = torch.reshape(torch.tensor(input_label), [1, 1, -1])\n"
			+ "        predicted_logits, predicted_iou = predictor.predict_masks(predictor.encoded_images,\n"
			+ "				input_points,\n"
			+ "				input_label,\n"
			+ "				multimask_output=True,\n"
			+ "				input_h=input_h,\n"
			+ "				input_w=input_w,\n"
			+ "				output_h=input_h,\n"
			+ "				output_w=input_w,)\n"
			+ "        sorted_ids = torch.argsort(predicted_iou, dim=-1, descending=True)\n"
			+ "        predicted_iou = torch.take_along_dim(predicted_iou, sorted_ids, dim=2)\n"
			+ "        predicted_logits = torch.take_along_dim(predicted_logits, sorted_ids[..., None, None], dim=2)\n"
			+ "        mask = torch.ge(predicted_logits[0, 0, 0, :, :], 0).cpu().detach().numpy()\n"
			+ "        if predicted_iou[0] > 1:\n"
			+ "            masks = np.concatenate((masks, mask.reshape(mask.shape[0], mask.shape[1], 1)), axis=1)\n"
			+ "            scores.append(predicted_iou[0].cpu().detach().numpy())\n"
			+ "\n"
			+ "    # TODO do we support detection of objects within objects?\n"
			+ "    pairs = calculate_pairs(masks)\n"
			+ "    while pairs.shape[0] != 0:\n"
			+ "        pp = pairs[0]\n"
			+ "        mask_sum = (masks[:, :, pp[0]] + masks[:, :, pp[1]])\n"
			+ "        union = (mask_sum > 0).sum()\n"
			+ "        intersec = (mask_sum == 2).sum()\n"
			+ "        if intersec / union > 0.8:\n"
			+ "            new_mask = (mask_sum > 0) * 1\n"
			+ "            masks = np.delete(masks, interest, axis=2)\n"
			+ "            masks = np.concatenate((masks, new_mask.reshape(masks.shape[0], masks.shape[1], 1)), axis=2)\n"
			+ "            pairs = calculate_pairs(masks)\n"
			+ "        elif intersec == masks[:, :, pp[0]].sum() or intersec == masks[:, :, pp[1]].sum():\n"
			+ "            pairs = np.concatenate((pairs, np.unique(np.array([pp[0], pp[1]])).reshape(-1, 2)), axis=0)\n"
			+ "        else:\n"
			+ "            ## TODO run again precition\n"
			+ "            if score[pp[0]] > score[pp[1]]:\n"
			+ "                (masks[:, :, pp[1]] = ((masks[:, :, pp[1]] - masks[:, :, pp[0]]) > 0) * 1\n"
			+ "            else:\n"
			+ "                (masks[:, :, pp[0]] = ((masks[:, :, pp[0]] - masks[:, :, pp[1]]) > 0) * 1\n"
			+ "            pairs = calculate_pairs(masks)\n"
			+ "        added_masks = masks.sum(2)\n"
			+ "        inds = np.where(added_masks > 1)\n"
			+ "    label = np.arange(masks.shape[2])\n"
			+ "    return label.reshape(1, 1, -1) * masks";
}
